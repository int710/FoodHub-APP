package com.example.foodhubapp.feature.menu.data

import com.example.foodhubapp.core.network.FoodHubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MenuSyncResult(
    val categoriesCount: Int,
    val menuItemsCount: Int
)

interface MenuRepository {
    suspend fun syncMenu(): MenuSyncResult
}

class RemoteMenuRepository(
    private val apiClient: FoodHubApiClient = FoodHubApiClient()
) : MenuRepository {
    override suspend fun syncMenu(): MenuSyncResult = withContext(Dispatchers.IO) {
        val categories = apiClient.get("/menu/categories")
        val menu = apiClient.get("/menu/all")

        MenuSyncResult(
            categoriesCount = categories.dataCount,
            menuItemsCount = menu.dataCount
        )
    }
}
