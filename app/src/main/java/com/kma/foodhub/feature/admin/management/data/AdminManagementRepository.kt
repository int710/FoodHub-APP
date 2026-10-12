package com.kma.foodhub.feature.admin.management.data

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kma.foodhub.core.datastore.TokenStore
import com.kma.foodhub.core.network.*
import com.kma.foodhub.feature.admin.management.model.AdminReview
import com.kma.foodhub.feature.admin.management.model.SystemSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminManagementRepository(
    context: Context,
    private val apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient,
) {
    private val tokenStore = TokenStore(context.applicationContext)

    suspend fun getReviews(rating: Int? = null, search: String? = null): List<AdminReview> = withContext(Dispatchers.IO) {
        val root = apiClient.execute(apiClient.adminManagementApi.getReviews(headers(), rating = rating, search = search))
        (root.optArray("data") ?: JsonArray()).objects().map { review ->
            val customer = review.optObject("customer")
            val item = review.optObject("menuItem")
            AdminReview(
                id = review.optString("id"),
                rating = review.optInt("rating"),
                comment = review.optString("comment"),
                images = review.optArray("images")?.strings().orEmpty(),
                createdAt = review.optString("createdAt"),
                customerName = customer?.optString("name").orEmpty().ifBlank { "Khách hàng" },
                customerAvatar = customer?.optionalString("avatar"),
                menuItemId = item?.optString("id").orEmpty(),
                menuItemName = item?.optString("name").orEmpty().ifBlank { "Món ăn" },
                menuItemImage = item?.optionalString("image"),
                orderCode = review.optObject("order")?.optString("orderCode").orEmpty(),
            )
        }
    }

    suspend fun deleteReview(id: String) = withContext(Dispatchers.IO) {
        apiClient.execute(apiClient.adminManagementApi.deleteReview(headers(), id)); Unit
    }

    suspend fun getSettings(): SystemSetting = withContext(Dispatchers.IO) {
        apiClient.execute(apiClient.adminManagementApi.getSettings(headers())).getObject("data").toSetting()
    }

    suspend fun updateSettings(setting: SystemSetting): SystemSetting = withContext(Dispatchers.IO) {
        val body = JsonObject()
            .put("appName", setting.appName)
            .put("restaurantName", setting.restaurantName)
            .put("supportEmail", setting.supportEmail.ifBlank { null })
            .put("supportPhone", setting.supportPhone.ifBlank { null })
            .put("address", setting.address.ifBlank { null })
            .put("logoUrl", setting.logoUrl.ifBlank { null })
            .put("vatRate", setting.vatRate)
            .put("serviceFeeRate", setting.serviceFeeRate)
            .put("defaultDeliveryFee", setting.defaultDeliveryFee)
            .put("currency", setting.currency)
        apiClient.execute(apiClient.adminManagementApi.updateSettings(headers(), body)).getObject("data").toSetting()
    }

    private suspend fun headers(): Map<String, String> {
        val token = tokenStore.getAccessToken()?.takeIf(String::isNotBlank) ?: error("Vui lòng đăng nhập lại")
        return mapOf("Authorization" to "Bearer $token")
    }
}

private fun JsonObject.toSetting() = SystemSetting(
    appName = optString("appName", "FoodHub"),
    restaurantName = optString("restaurantName", "FoodHub"),
    supportEmail = optionalString("supportEmail").orEmpty(),
    supportPhone = optionalString("supportPhone").orEmpty(),
    address = optionalString("address").orEmpty(),
    logoUrl = optionalString("logoUrl").orEmpty(),
    vatRate = optDouble("vatRate", 10.0),
    serviceFeeRate = optDouble("serviceFeeRate", 0.0),
    defaultDeliveryFee = optLong("defaultDeliveryFee"),
    currency = optString("currency", "VND"),
)

private fun JsonObject.optionalString(key: String) = if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
private fun JsonArray.objects() = (0 until size()).mapNotNull(::optObject)
private fun JsonArray.strings() = (0 until size()).mapNotNull { opt(it)?.toString()?.takeIf(String::isNotBlank) }
