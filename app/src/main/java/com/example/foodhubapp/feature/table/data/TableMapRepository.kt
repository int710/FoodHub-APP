package com.example.foodhubapp.feature.table.data

import com.example.foodhubapp.core.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonObject

enum class RestaurantTableStatus { AVAILABLE, OCCUPIED, INACTIVE }

data class RestaurantTable(
    val id: String,
    val name: String,
    val capacity: Int,
    val floor: String?,
    val note: String?,
    val status: RestaurantTableStatus
)

interface TableMapRepository {
    suspend fun getTables(): List<RestaurantTable>
}

class RemoteTableMapRepository(
    private val apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient
) : TableMapRepository {
    override suspend fun getTables(): List<RestaurantTable> = withContext(Dispatchers.IO) {
        val data = apiClient.execute(apiClient.tableApi.getTables()).optArray("data")
            ?: error("Máy chủ chưa trả danh sách bàn.")
        (0 until data.size()).mapNotNull { index ->
            data.optObject(index)?.toRestaurantTable()
        }
    }
}

private fun JsonObject.toRestaurantTable() = RestaurantTable(
    id = optString("id"),
    name = optString("name").ifBlank { "Bàn" },
    capacity = optInt("capacity", 1).coerceAtLeast(1),
    floor = optionalString("floor"),
    note = optionalString("note"),
    status = runCatching { RestaurantTableStatus.valueOf(optString("status")) }
        .getOrDefault(if (optBoolean("isActive", true)) RestaurantTableStatus.AVAILABLE else RestaurantTableStatus.INACTIVE)
)

private fun JsonObject.optionalString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
