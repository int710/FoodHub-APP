package com.example.foodhubapp.feature.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.cart.data.CartRepository
import com.example.foodhubapp.feature.cart.data.CartContextStore
import com.example.foodhubapp.feature.cart.data.RemoteCartRepository
import com.example.foodhubapp.feature.menu.data.FoodRepository
import com.example.foodhubapp.feature.menu.data.MenuCategory
import com.example.foodhubapp.feature.menu.data.MenuFood
import com.example.foodhubapp.feature.menu.data.RemoteFoodRepository
import com.example.foodhubapp.feature.menu.viewmodel.foodError
import com.example.foodhubapp.feature.table.data.RemoteTableMapRepository
import com.example.foodhubapp.feature.table.data.RestaurantTable
import com.example.foodhubapp.feature.table.data.TableMapRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.foodhub.app.TableSessionStore
import com.foodhub.app.TableSession
import com.example.foodhubapp.feature.cart.data.CartType

data class HomeUiState(
    val isLoading: Boolean = false,
    val categories: List<MenuCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val query: String = "",
    val cartItemCount: Int = 0,
    val tables: List<RestaurantTable> = emptyList(),
    val tableError: String? = null,
    val userName: String? = null,
    val isAuthenticated: Boolean = false,
    val tableSession: TableSession? = null,
    val cartType: CartType = CartType.TAKEAWAY,
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
        tableToken = { TableSessionStore(application.applicationContext).current()?.tableToken },
        accessToken = TokenStore(application.applicationContext)::getAccessToken
    ),
    private val tableRepository: TableMapRepository = RemoteTableMapRepository()
) : AndroidViewModel(application) {
    private val tokenStore = TokenStore(application.applicationContext)
    private val tableSessionStore = TableSessionStore(application.applicationContext)
    private val cartContextStore = CartContextStore(application.applicationContext)
    private val state = MutableStateFlow(HomeUiState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            tokenStore.user.collect { user -> state.update { it.copy(userName = user?.fullName) } }
        }
        viewModelScope.launch {
            tokenStore.accessToken.collect { token -> state.update { it.copy(isAuthenticated = !token.isNullOrBlank()) } }
        }
        load()
    }

    fun load() {
        if (state.value.isLoading) return
        val tableSession = tableSessionStore.current()
        val cartType = cartContextStore.currentType()
        state.update { it.copy(isLoading = true, error = null, tableSession = tableSession, cartType = cartType) }
        viewModelScope.launch {
            try {
                val categories = foodRepository.getMenu()
                val cartCount = runCatching { cartRepository.getCart(cartType).items.sumOf { it.quantity } }.getOrDefault(0)
                val tableResult = runCatching { tableRepository.getTables() }
                state.update { current ->
                    current.copy(
                        categories = categories,
                        cartItemCount = cartCount,
                        tables = tableResult.getOrDefault(current.tables),
                        tableError = tableResult.exceptionOrNull()?.let { error ->
                            foodError(error as? Exception ?: Exception(error))
                        }
                    )
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

    fun selectCartType(type: CartType) {
        if (type == CartType.DINE_IN) return
        tableSessionStore.clear()
        cartContextStore.selectAccountType(type)
        state.update { it.copy(tableSession = null, cartType = type, cartItemCount = 0) }
        load()
    }
}
