package com.example.foodhubapp.feature.admin.menu.data

import android.content.Context
import android.net.Uri
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.admin.model.AdminMenuCategory
import com.example.foodhubapp.feature.admin.model.AdminMenuCategoryInput
import com.example.foodhubapp.feature.admin.model.AdminMenuInput
import com.example.foodhubapp.feature.admin.model.AdminMenuItem
import com.example.foodhubapp.feature.admin.model.AdminFlashSaleInput
import com.example.foodhubapp.feature.admin.model.AdminVariantInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AdminMenuRepository private constructor(
    private val appContext: Context?,
    private val apiClient: FoodHubApiClient,
    private val accessToken: suspend () -> String?,
) {
    constructor(context: Context, apiClient: FoodHubApiClient = FoodHubApiClient()) : this(
        context.applicationContext, apiClient, { TokenStore(context.applicationContext).getAccessToken() },
    )
    internal constructor(apiClient: FoodHubApiClient, accessToken: suspend () -> String?) : this(null, apiClient, accessToken)

    suspend fun uploadImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = requireNotNull(appContext).contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Không đọc được ảnh đã chọn")
        require(bytes.size <= 5 * 1024 * 1024) { "Ảnh phải nhỏ hơn hoặc bằng 5 MB" }
        val mimeType = resolver.getType(uri)?.takeIf { it.startsWith("image/") }
            ?: throw IllegalArgumentException("Tệp đã chọn không phải là ảnh")
        val response = apiClient.uploadImage(
            path = "/media/upload-image",
            fileName = "menu-${System.currentTimeMillis()}.${mimeType.substringAfter('/')}",
            mimeType = mimeType,
            bytes = bytes,
            headers = authHeaders(),
        )
        response.optJSONArray("data")?.optJSONObject(0)?.optString("url")
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Máy chủ không trả về đường dẫn ảnh")
    }

    suspend fun getItems(): List<AdminMenuItem> = withContext(Dispatchers.IO) {
        val response = apiClient.getJson(
            path = "/menu/items?page=1&limit=100",
            headers = authHeaders(),
        )
        val payload = response.optJSONObject("data") ?: JSONObject()
        val items = payload.optJSONArray("data") ?: JSONArray()
        items.objects().map(JSONObject::toAdminMenuItem)
    }

    suspend fun getCategories(): List<AdminMenuCategory> = withContext(Dispatchers.IO) {
        val response = apiClient.getJson("/menu/categories")
        (response.optJSONArray("data") ?: JSONArray()).objects().map { category ->
            AdminMenuCategory(
                id = category.getString("id"),
                name = category.getString("name"),
                icon = category.optString("icon").ifBlank { "🍽" },
                sortOrder = category.optInt("sortOrder", 0),
                isActive = category.optBoolean("isActive", true),
                itemCount = category.optJSONObject("_count")?.optInt("item", 0) ?: 0,
            )
        }
    }

    suspend fun createCategory(input: AdminMenuCategoryInput) = withContext(Dispatchers.IO) {
        apiClient.post("/menu/categories", input.toJson(), authHeaders())
        Unit
    }

    suspend fun updateCategory(id: String, input: AdminMenuCategoryInput) = withContext(Dispatchers.IO) {
        apiClient.put("/menu/categories/$id", input.toJson(), authHeaders())
        Unit
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        apiClient.delete("/menu/categories/$id", authHeaders())
        Unit
    }

    suspend fun createItem(input: AdminMenuInput) = withContext(Dispatchers.IO) {
        apiClient.post("/menu/items", input.toJson(), authHeaders())
        Unit
    }

    suspend fun updateItem(id: String, input: AdminMenuInput) = withContext(Dispatchers.IO) {
        apiClient.patch("/menu/items/$id", input.toJson(), authHeaders())
        Unit
    }

    suspend fun toggleItem(id: String) = withContext(Dispatchers.IO) {
        apiClient.patch("/menu/items/$id/toggle", JSONObject(), authHeaders())
        Unit
    }

    suspend fun deleteItem(id: String): String = withContext(Dispatchers.IO) {
        val response = apiClient.delete("/menu/items/$id", authHeaders())
        response.optJSONObject("data")?.optString("message")
            ?.takeIf(String::isNotBlank)
            ?: response.optString("message", "Đã xử lý món ăn")
    }

    suspend fun createFlashSale(input: AdminFlashSaleInput) = withContext(Dispatchers.IO) {
        val startsAt = java.time.Instant.now()
        apiClient.post(
            "/menu/item/flash-sales",
            JSONObject()
                .put("itemId", input.itemId)
                .put("discountPercent", input.discountPercent)
                .put("startsAt", startsAt.toString())
                .put("endsAt", startsAt.plusSeconds(input.durationHours * 3600L).toString())
                .put("isActive", true),
            authHeaders(),
        )
        Unit
    }

    suspend fun deleteFlashSale(itemId: String) = withContext(Dispatchers.IO) {
        apiClient.delete("/menu/item/flash-sales/$itemId", authHeaders())
        Unit
    }

    suspend fun createVariant(itemId: String, input: AdminVariantInput) = withContext(Dispatchers.IO) {
        val options = JSONArray()
        input.options.forEachIndexed { index, option ->
            options.put(JSONObject().put("name", option.name.trim()).put("priceAdd", option.priceAdd)
                .put("sortOrder", index).put("isActive", true))
        }
        apiClient.post(
            "/menu/items/$itemId/variants",
            JSONObject().put("name", input.name.trim())
                .put("type", if (input.multiple) "MULTIPLE" else "SINGLE")
                .put("isRequired", input.required).put("sortOrder", input.sortOrder).put("options", options),
            authHeaders(),
        )
        Unit
    }

    suspend fun getVariantGroups(itemId: String): List<com.example.foodhubapp.feature.admin.model.AdminVariantGroup> = withContext(Dispatchers.IO) {
        val data = apiClient.getJson("/menu/item/$itemId").getJSONObject("data")
        val groups = data.optJSONArray("variantGroups") ?: JSONArray()
        (0 until groups.length()).map { index ->
            val group = groups.getJSONObject(index)
            val options = group.optJSONArray("options") ?: JSONArray()
            com.example.foodhubapp.feature.admin.model.AdminVariantGroup(group.getString("id"), AdminVariantInput(
                group.getString("name"), group.optString("type") == "MULTIPLE", group.optBoolean("isRequired"),
                (0 until options.length()).map { i ->
                    val option = options.getJSONObject(i)
                    com.example.foodhubapp.feature.admin.model.AdminVariantOptionInput(option.getString("name"), option.money("priceAdd"), option.getString("id"))
                },
                sortOrder = group.optInt("sortOrder"),
            ))
        }
    }

    suspend fun updateVariant(groupId: String, input: AdminVariantInput) = withContext(Dispatchers.IO) {
        val options = JSONArray()
        input.options.forEachIndexed { index, option ->
            val value = JSONObject().put("name", option.name.trim()).put("priceAdd", option.priceAdd).put("sortOrder", index)
            option.id?.let { value.put("id", it) }
            options.put(value)
        }
        apiClient.patch("/menu/item-variants/$groupId", JSONObject().put("name", input.name.trim())
            .put("type", if (input.multiple) "MULTIPLE" else "SINGLE").put("isRequired", input.required)
            .put("sortOrder", input.sortOrder).put("options", options), authHeaders())
        Unit
    }

    private suspend fun authHeaders(): Map<String, String> {
        val token = accessToken()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Vui lòng đăng nhập bằng tài khoản Admin")
        return mapOf("Authorization" to "Bearer $token")
    }
}

private fun AdminMenuCategoryInput.toJson() = JSONObject()
    .put("name", name.trim())
    .put("icon", icon.trim())
    .put("sortOrder", sortOrder)

private fun AdminMenuInput.toJson() = JSONObject()
    .put("categoryId", categoryId)
    .put("name", name.trim())
    .put("description", description?.trim()?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
    .put("basePrice", basePrice)
    .put("image", imageUrl?.trim()?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
    .put("isAvailable", isAvailable)
    .put("isFeatured", isFeatured)
    .put("sortOrder", sortOrder)

private fun JSONObject.toAdminMenuItem(): AdminMenuItem {
    val category = optJSONObject("category")
    val basePrice = money("basePrice")
    val sale = optJSONObject("flashSale")
    val salePrice = sale?.takeIf { it.optBoolean("isActive", true) }
        ?.optDouble("discountPercent")
        ?.takeIf { it > 0.0 }
        ?.let { discount -> (basePrice * (1.0 - discount / 100.0)).toLong() }
    val name = optString("name")
    return AdminMenuItem(
        id = getString("id"),
        name = name,
        category = category?.optString("name").orEmpty().ifBlank { "Chưa phân loại" },
        price = basePrice,
        salePrice = salePrice,
        isAvailable = optBoolean("isAvailable", true),
        isFeatured = optBoolean("isFeatured", false),
        icon = name.firstOrNull()?.uppercase() ?: "M",
        categoryId = optString("categoryId", category?.optString("id").orEmpty()),
        description = optionalString("description"),
        imageUrl = optionalString("image"),
        sortOrder = optInt("sortOrder", 0),
    )
}

private fun JSONObject.money(key: String): Long = when (val value = opt(key)) {
    is Number -> value.toDouble().toLong()
    is String -> value.toBigDecimalOrNull()?.toLong() ?: 0L
    else -> 0L
}

private fun JSONObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

private fun JSONArray.objects(): List<JSONObject> =
    (0 until length()).mapNotNull { index -> optJSONObject(index) }
