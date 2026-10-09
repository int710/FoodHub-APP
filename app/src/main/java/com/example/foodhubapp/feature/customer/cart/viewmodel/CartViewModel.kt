package com.example.foodhubapp.feature.customer.cart.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.feature.customer.cart.data.Cart
import com.example.foodhubapp.feature.customer.cart.data.CartItem
import com.example.foodhubapp.feature.customer.cart.data.CartItemUpdate
import com.example.foodhubapp.feature.customer.cart.data.CartRepository
import com.example.foodhubapp.feature.customer.cart.data.CartType
import com.example.foodhubapp.feature.customer.cart.data.CheckoutRequest
import com.example.foodhubapp.feature.customer.cart.data.CheckoutResult
import com.example.foodhubapp.feature.customer.cart.data.RemoteCartRepository
import com.example.foodhubapp.feature.customer.menu.data.FoodRepository
import com.example.foodhubapp.feature.customer.menu.data.LoginRequiredException
import com.example.foodhubapp.feature.customer.menu.data.RemoteFoodRepository
import com.example.foodhubapp.feature.customer.menu.ui.FoodDetail
import com.example.foodhubapp.feature.customer.order.data.OrderingContextStore
import com.example.foodhubapp.feature.customer.table.data.TableSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Toàn bộ trạng thái mà CartRoute cần render. `busyItemId` khóa riêng dòng đang
 * cập nhật, còn `isClearing` khóa thao tác trên toàn bộ giỏ.
 */
data class CartUiState(
    val isLoading: Boolean = false,
    val cart: Cart = Cart(emptyList(), 0),
    val error: String? = null,
    val busyItemId: String? = null,
    val isClearing: Boolean = false,
    val message: String? = null,
    val requiresLogin: Boolean = false,
    val editingItem: CartItem? = null,
    val editingFood: FoodDetail? = null,
    val isLoadingEditor: Boolean = false,
    val cartType: CartType = CartType.TAKEAWAY,
    val isCheckingOut: Boolean = false,
    val checkoutResult: CheckoutResult? = null,
)

/**
 * Điều phối luồng UI -> Repository -> StateFlow. ViewModel không tự tạo JSON
 * hoặc gọi HTTP; các chi tiết đó thuộc CartRepository.
 */
class CartViewModel @JvmOverloads constructor(
    application: Application,
    private val cartRepository: CartRepository = RemoteCartRepository(
        accessToken = TokenStore(application.applicationContext)::getAccessToken,
        tableToken = { TableSessionStore(application.applicationContext).current()?.tableToken },
    ),
    private val foodRepository: FoodRepository = RemoteFoodRepository()
) : AndroidViewModel(application) {
    private val orderingContextStore = OrderingContextStore(application.applicationContext)
    private val state = MutableStateFlow(CartUiState(cartType = orderingContextStore.currentType()))
    val uiState = state.asStateFlow()

    init { load() }

    /** Bước đầu của màn cart: GET giỏ TAKEAWAY rồi phát kết quả qua uiState. */
    fun load() {
        if (state.value.isLoading || state.value.busyItemId != null || state.value.isClearing) return
        state.update { it.copy(isLoading = true, error = null, requiresLogin = false) }
        viewModelScope.launch {
            try {
                state.update { it.copy(cart = cartRepository.getCart(it.cartType)) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error, loadError = true)
            } finally {
                state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun changeQuantity(item: CartItem, quantity: Int) {
        if (quantity !in 1..99) return
        update(item.id, CartItemUpdate(quantity = quantity), "Đã cập nhật số lượng.")
    }

    fun selectType(type: CartType) {
        if (type == state.value.cartType || state.value.isLoading || state.value.isCheckingOut) return
        if (orderingContextStore.currentType() == CartType.DINE_IN && type != CartType.DINE_IN) {
            state.update { it.copy(message = "Hãy kết thúc phiên bàn trước khi chuyển loại đơn.") }
            return
        }
        runCatching { orderingContextStore.select(type) }
            .onFailure { state.update { current -> current.copy(message = it.message) } }
            .onSuccess {
                state.update { current -> current.copy(cartType = type, cart = Cart(emptyList(), 0), error = null) }
                load()
            }
    }

    fun edit(item: CartItem) {
        if (state.value.busyItemId != null || state.value.isLoadingEditor) return
        if (item.menuItemId.isBlank()) {
            state.update { it.copy(message = "Không tìm thấy thông tin món để chỉnh sửa.") }
            return
        }
        state.update { it.copy(editingItem = item, editingFood = null, isLoadingEditor = true) }
        viewModelScope.launch {
            try {
                // Cart response không bảo đảm có toàn bộ nhóm option. Tải chi tiết món
                // để dialog sửa dùng đúng SINGLE/MULTIPLE và nhóm bắt buộc hiện tại.
                val food = foodRepository.getFood(item.menuItemId)
                state.update { it.copy(editingFood = food) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(message = cartError(error), editingItem = null) }
            } finally {
                state.update { it.copy(isLoadingEditor = false) }
            }
        }
    }

    fun saveEdit(item: CartItem, quantity: Int, optionIds: List<String>, note: String) {
        val food = state.value.editingFood ?: return
        // Kiểm tra giống màn chi tiết trước khi gửi PATCH để phản hồi lỗi ngay trên UI.
        val valid = quantity in 1..99 && note.length <= 255 && optionIds.distinct().size == optionIds.size &&
            optionIds.all { id -> food.groups.any { group -> group.options.any { it.id == id } } } &&
            food.groups.all { group ->
                val count = group.options.count { it.id in optionIds }
                (!group.required || count > 0) && (group.multiple || count <= 1)
            }
        if (!valid) {
            state.update { it.copy(message = "Vui lòng chọn đủ tùy chọn bắt buộc.") }
            return
        }
        state.update { it.copy(editingItem = null, editingFood = null) }
        update(
            item.id,
            CartItemUpdate(quantity, optionIds, note.trim()),
            "Đã cập nhật món trong giỏ."
        )
    }

    fun dismissEditor() {
        if (!state.value.isLoadingEditor) state.update { it.copy(editingItem = null, editingFood = null) }
    }

    fun delete(item: CartItem) {
        runItemAction(item.id, "Đã xóa món khỏi giỏ.") { cartRepository.deleteItem(item.id, state.value.cartType) }
    }

    fun clear() {
        val current = state.value
        if (current.isLoading || current.isClearing || current.busyItemId != null || current.cart.items.isEmpty()) return
        state.update { it.copy(isClearing = true, message = null) }
        viewModelScope.launch {
            try {
                cartRepository.clear(current.cartType)
                state.update { it.copy(cart = Cart(emptyList(), 0), message = "Đã xóa toàn bộ giỏ hàng.") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            } finally {
                state.update { it.copy(isClearing = false) }
            }
        }
    }

    private fun update(itemId: String, update: CartItemUpdate, successMessage: String) {
        runItemAction(itemId, successMessage) { cartRepository.updateItem(itemId, update, state.value.cartType) }
    }

    private fun runItemAction(itemId: String, successMessage: String, action: suspend () -> Unit) {
        val current = state.value
        if (current.isLoading || current.isClearing || current.busyItemId != null) return
        state.update { it.copy(busyItemId = itemId, message = null) }
        viewModelScope.launch {
            try {
                action()
                // Luôn GET lại sau PATCH/DELETE. Giá và tổng tiền phải lấy từ backend,
                // không tự tính rồi coi đó là kết quả chính thức.
                val cart = cartRepository.getCart(state.value.cartType)
                state.update { it.copy(cart = cart, message = successMessage) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            } finally {
                state.update { it.copy(busyItemId = null) }
            }
        }
    }

    private fun handleError(error: Exception, loadError: Boolean = false) {
        // 401 và thiếu token cùng đi vào một luồng đăng nhập thống nhất.
        if (error is LoginRequiredException ||
            (error is FoodHubApiException && error.statusCode == 401 && state.value.cartType != CartType.DINE_IN)
        ) {
            state.update { it.copy(requiresLogin = true, error = if (loadError) null else it.error) }
        } else if (loadError) {
            state.update { it.copy(error = cartError(error)) }
        } else {
            state.update { it.copy(message = cartError(error)) }
        }
    }

    fun dismissLogin() { state.update { it.copy(requiresLogin = false) } }
    fun consumeMessage() { state.update { it.copy(message = null) } }

    fun checkout(request: CheckoutRequest) {
        val current = state.value
        if (current.cart.items.isEmpty() || current.isCheckingOut || request.type != current.cartType) return
        state.update { it.copy(isCheckingOut = true, message = null, checkoutResult = null) }
        viewModelScope.launch {
            try {
                val result = cartRepository.checkout(request)
                state.update {
                    it.copy(
                        cart = Cart(emptyList(), 0),
                        checkoutResult = result,
                        message = "Đã tạo đơn ${result.orderCode}.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            } finally {
                state.update { it.copy(isCheckingOut = false) }
            }
        }
    }

    fun consumeCheckoutResult() { state.update { it.copy(checkoutResult = null) } }
}

internal fun cartError(error: Exception): String = when (error) {
    is SocketTimeoutException -> "Máy chủ phản hồi quá lâu. Vui lòng thử lại."
    is IOException -> "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng."
    is FoodHubApiException -> when (error.statusCode) {
        404 -> "Món trong giỏ không còn tồn tại."
        422 -> error.message ?: "Dữ liệu giỏ hàng chưa hợp lệ."
        else -> error.message ?: "Không thể cập nhật giỏ hàng."
    }
    else -> error.message ?: "Không thể cập nhật giỏ hàng."
}
