package com.example.foodhubapp.feature.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.cart.data.CartRepository
import com.example.foodhubapp.feature.cart.data.RemoteCartRepository
import com.example.foodhubapp.feature.menu.data.FoodRepository
import com.example.foodhubapp.feature.menu.data.MenuCategory
import com.example.foodhubapp.feature.menu.data.MenuFood
import com.example.foodhubapp.feature.menu.data.RemoteFoodRepository
import com.example.foodhubapp.feature.menu.viewmodel.foodError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val categories: List<MenuCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val query: String = "",
    val cartItemCount: Int = 0,
    val userName: String? = null,
    val error: String? = null
) {
    val visibleFoods: List<MenuFood>
        get() = categories
            .filter { selectedCategoryId == null || it.id == selectedCategoryId }
            .flatMap { it.items }
            .filter { food -> query.isBlank() || food.name.contains(query.trim(), ignoreCase = true) }

    val popularFoods: List<MenuFood>
        get() {
            val all = categories.flatMap { it.items }.filter { it.available }
            val featured = all.filter { it.isFeatured }.sortedByDescending { it.totalOrder }
            return (featured.ifEmpty { all.sortedByDescending { it.totalOrder } }).take(5)
        }
}

/**
 * Home dùng menu public làm nguồn dữ liệu chính. Cart chỉ được tải thêm khi đã
 * có token; lỗi cart không được làm mất nội dung menu dành cho khách.
 */
class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val foodRepository: FoodRepository = RemoteFoodRepository(),
    private val cartRepository: CartRepository = RemoteCartRepository(
        accessToken = TokenStore(application.applicationContext)::getAccessToken
    )
) : AndroidViewModel(application) {
    private val tokenStore = TokenStore(application.applicationContext)
    private val state = MutableStateFlow(HomeUiState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            tokenStore.user.collect { user -> state.update { it.copy(userName = user?.fullName) } }
        }
        load()
    }

    fun load() {
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val categories = foodRepository.getMenu()
                val cartCount = runCatching { cartRepository.getCart().items.sumOf { it.quantity } }.getOrDefault(0)
                state.update { current ->
                    current.copy(categories = categories, cartItemCount = cartCount)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(error = foodError(error)) }
            } finally {
                state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onQueryChange(query: String) { state.update { it.copy(query = query) } }
    fun selectCategory(categoryId: String?) { state.update { it.copy(selectedCategoryId = categoryId) } }
}
