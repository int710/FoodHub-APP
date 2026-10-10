package com.example.foodhubapp.feature.admin.menu.data

import com.example.foodhubapp.core.network.*
import android.content.Context
import android.net.Uri
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.admin.model.AdminMenuCategory
import com.example.foodhubapp.feature.admin.model.AdminMenuCategoryInput
import com.example.foodhubapp.feature.admin.model.AdminMenuInput
import com.example.foodhubapp.feature.admin.model.AdminMenuItem
import com.example.foodhubapp.feature.admin.model.AdminFlashSaleInput
import com.example.foodhubapp.feature.admin.model.AdminVariantInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class AdminMenuRepository private constructor(
    private val appContext: Context?,
    private val apiClient: FoodHubApiClient,
    private val accessToken: suspend () -> String?,
) {
    constructor(context: Context, apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient) : this(
        context.applicationContext, apiClient, { TokenStore(context.applicationContext).getAccessToken() },
    )
    internal constructor(apiClient: FoodHubApiClient, accessToken: suspend () -> String?) : this(null, apiClient, accessToken)

    suspend fun uploadImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val token = accessToken()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Vui lòng đăng nhập bằng tài khoản Admin")
        uploadImageFromDevice(
            context = requireNotNull(appContext),
            uri = uri,
            accessToken = token,
            filePrefix = "menu",
            apiClient = apiClient,
        )
    }

    suspend fun getItems(): List<AdminMenuItem> = withContext(Dispatchers.IO) {
        val response = apiClient.execute(
            apiClient.adminMenuApi.getItems(
                headers = authHeaders()
            )
        )
        val payload = response.optObject("data") ?: JsonObject()
        val items = payload.optArray("data") ?: JsonArray()
        items.objects().map(JsonObject::toAdminMenuItem)
    }

    suspend fun getCategories(): List<AdminMenuCategory> = withContext(Dispatchers.IO) {
        val response = apiClient.execute(apiClient.adminMenuApi.getCategories())
        (response.optArray("data") ?: JsonArray()).objects().map { category ->
            AdminMenuCategory(
                id = category.getString("id"),
                name = category.getString("name"),
                icon = category.optString("icon").ifBlank { "🍽" },
                sortOrder = category.optInt("sortOrder", 0),
                isActive = category.optBoolean("isActive", true),
                itemCount = category.optObject("_count")?.optInt("item", 0) ?: 0,
            )
        }
    }

    suspend fun createCategory(input: AdminMenuCategoryInput) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.adminMenuApi.createCategory(
                headers = authHeaders(),
                body = input.toJson()
            )
        )
        Unit
    }

    suspend fun updateCategory(id: String, input: AdminMenuCategoryInput) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.adminMenuApi.updateCategory(
                id = id,
                headers = authHeaders(),
                body = input.toJson()
            )
        )
        Unit
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.adminMenuApi.deleteCategory(
                id = id,
                headers = authHeaders()
            )
        )
        Unit
    }

    suspend fun createItem(input: AdminMenuInput) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.adminMenuApi.createItem(
                headers = authHeaders(),
                body = input.toJson()
            )
        )
        Unit
    }

    suspend fun updateItem(id: String, input: AdminMenuInput) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.adminMenuApi.updateItem(
                id = id,
                headers = authHeaders(),
                body = input.toJson()
            )
        )
        Unit
    }

    suspend fun toggleItem(id: String) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.adminMenuApi.toggleItem(
                id = id,
                headers = authHeaders(),
                body = JsonObject()
            )
        )
        Unit
    }

    suspend fun deleteItem(id: String): String = withContext(Dispatchers.IO) {
        val response = apiClient.execute(
            apiClient.adminMenuApi.deleteItem(
                id = id,
                headers = authHeaders()
            )
        )
        response.optObject("data")?.optString("message")
            ?.takeIf(String::isNotBlank)
            ?: response.optString("message", "Đã xử lý món ăn")
    }

    suspend fun createFlashSale(input: AdminFlashSaleInput) = withContext(Dispatchers.IO) {
        val startsAt = java.time.Instant.now()
        apiClient.execute(
            apiClient.adminMenuApi.createFlashSale(
                headers = authHeaders(),
                body = JsonObject()
                    .put("itemId", input.itemId)
                    .put("discountPercent", input.discountPercent)
                    .put("startsAt", startsAt.toString())
                    .put("endsAt", startsAt.plusSeconds(input.durationHours * 3600L).toString())
                    .put("isActive", true)
            )
        )
        Unit
    }

    suspend fun deleteFlashSale(itemId: String) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.adminMenuApi.deleteFlashSale(
                id = itemId,
                headers = authHeaders()
            )
        )
        Unit
    }

    suspend fun createVariant(itemId: String, input: AdminVariantInput) = withContext(Dispatchers.IO) {
        val options = JsonArray()
        input.options.forEachIndexed { index, option ->
            options.put(JsonObject().put("name", option.name.trim()).put("priceAdd", option.priceAdd)
                .put("sortOrder", index).put("isActive", true))
        }
        apiClient.execute(
            apiClient.adminMenuApi.createVariant(
                id = itemId,
                headers = authHeaders(),
                body = JsonObject().put("name", input.name.trim())
                    .put("type", if (input.multiple) "MULTIPLE" else "SINGLE")
                    .put("isRequired", input.required).put("sortOrder", input.sortOrder).put("options", options)
            )
        )
        Unit
    }

    suspend fun getVariantGroups(itemId: String): List<com.example.foodhubapp.feature.admin.model.AdminVariantGroup> = withContext(Dispatchers.IO) {
        val data = apiClient.execute(
            apiClient.adminMenuApi.getItem(
                id = itemId
            )
        ).getObject("data")
        val groups = data.optArray("variantGroups") ?: JsonArray()
        (0 until groups.size()).map { index ->
            val group = groups.getObject(index)
            val options = group.optArray("options") ?: JsonArray()
            com.example.foodhubapp.feature.admin.model.AdminVariantGroup(group.getString("id"), AdminVariantInput(
                group.getString("name"), group.optString("type") == "MULTIPLE", group.optBoolean("isRequired"),
                (0 until options.size()).map { i ->
                    val option = options.getObject(i)
                    com.example.foodhubapp.feature.admin.model.AdminVariantOptionInput(option.getString("name"), option.money("priceAdd"), option.getString("id"))
                },
                sortOrder = group.optInt("sortOrder"),
            ))
        }
    }

    suspend fun updateVariant(groupId: String, input: AdminVariantInput) = withContext(Dispatchers.IO) {
        val options = JsonArray()
        input.options.forEachIndexed { index, option ->
            val value = JsonObject().put("name", option.name.trim()).put("priceAdd", option.priceAdd).put("sortOrder", index)
            option.id?.let { value.put("id", it) }
            options.put(value)
        }
        apiClient.execute(
            apiClient.adminMenuApi.updateVariant(
                id = groupId,
                headers = authHeaders(),
                body = JsonObject().put("name", input.name.trim())
                .put("type", if (input.multiple) "MULTIPLE" else "SINGLE").put("isRequired", input.required)
                .put("sortOrder", input.sortOrder).put("options", options)
            )
        )
        Unit
    }

    private suspend fun authHeaders(): Map<String, String> {
        val token = accessToken()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Vui lòng đăng nhập bằng tài khoản Admin")
        return mapOf("Authorization" to "Bearer $token")
    }
}

private fun AdminMenuCategoryInput.toJson() = JsonObject()
    .put("name", name.trim())
    .put("icon", icon.trim())
    .put("sortOrder", sortOrder)

private fun AdminMenuInput.toJson() = JsonObject()
    .put("categoryId", categoryId)
    .put("name", name.trim())
    .put("description", description?.trim()?.takeIf(String::isNotBlank) ?: com.google.gson.JsonNull.INSTANCE)
    .put("basePrice", basePrice)
    .put("image", imageUrl?.trim()?.takeIf(String::isNotBlank) ?: com.google.gson.JsonNull.INSTANCE)
    .put("isAvailable", isAvailable)
    .put("isFeatured", isFeatured)
    .put("sortOrder", sortOrder)

private fun JsonObject.toAdminMenuItem(): AdminMenuItem {
    val category = optObject("category")
    val basePrice = money("basePrice")
    val sale = optObject("flashSale")
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

private fun JsonObject.money(key: String): Long = when (val value = opt(key)) {
    is Number -> value.toDouble().toLong()
    is String -> value.toBigDecimalOrNull()?.toLong() ?: 0L
    else -> 0L
}

private fun JsonObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

private fun JsonArray.objects(): List<JsonObject> =
    (0 until size()).mapNotNull { index -> optObject(index) }
