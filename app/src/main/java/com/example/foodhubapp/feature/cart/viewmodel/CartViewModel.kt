package com.example.foodhubapp.feature.cart.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.feature.cart.data.Cart
import com.example.foodhubapp.feature.cart.data.CartContextStore
import com.example.foodhubapp.feature.cart.data.CartItem
import com.example.foodhubapp.feature.cart.data.CartItemUpdate
import com.example.foodhubapp.feature.cart.data.CartRepository
import com.example.foodhubapp.feature.cart.data.CartType
import com.example.foodhubapp.feature.cart.data.RemoteCartRepository
import com.example.foodhubapp.feature.menu.data.FoodRepository
import com.example.foodhubapp.feature.menu.data.LoginRequiredException
import com.example.foodhubapp.feature.menu.data.RemoteFoodRepository
import com.example.foodhubapp.feature.menu.ui.FoodDetail
import com.example.foodhubapp.feature.order.data.OrderRepository
import com.example.foodhubapp.feature.order.data.RemoteOrderRepository
import com.example.foodhubapp.feature.order.data.OrderType
import com.example.foodhubapp.feature.order.data.CheckoutPaymentMethod
import com.foodhub.app.TableSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
    val requiresTableScan: Boolean = false,
    val editingItem: CartItem? = null,
    val editingFood: FoodDetail? = null,
    val isLoadingEditor: Boolean = false,
    val isCheckingOut: Boolean = false,
    val paymentUrl: String? = null,
    val paymentMethod: CheckoutPaymentMethod = CheckoutPaymentMethod.CASH,
    val cartType: CartType = CartType.TAKEAWAY
)

/**
 * Điều phối luồng UI -> Repository -> StateFlow. ViewModel không tự tạo JSON
 * hoặc gọi HTTP; các chi tiết đó thuộc CartRepository.
 */
class CartViewModel @JvmOverloads constructor(
    application: Application,
    private val cartRepository: CartRepository = RemoteCartRepository(
        tableToken = { TableSessionStore(application.applicationContext).current()?.tableToken },
        accessToken = TokenStore(application.applicationContext)::getAccessToken
    ),
    private val foodRepository: FoodRepository = RemoteFoodRepository(),
    private val orderRepository: OrderRepository = RemoteOrderRepository(
        tableToken = { TableSessionStore(application.applicationContext).current()?.tableToken },
        accessToken = TokenStore(application.applicationContext)::getAccessToken
    )
) : AndroidViewModel(application) {
    private val state = MutableStateFlow(CartUiState())
    private val tableSessionStore = TableSessionStore(application.applicationContext)
    private val cartContextStore = CartContextStore(application.applicationContext)
    val uiState = state.asStateFlow()

    private fun cartType() = cartContextStore.currentType()

    init { load() }

    /** Tải giỏ DINE_IN khi còn phiên QR, nếu không tải giỏ TAKEAWAY. */
    fun load() {
        if (state.value.isLoading || state.value.busyItemId != null || state.value.isClearing) return
        val type = cartType()
        state.update { it.copy(isLoading = true, error = null, requiresLogin = false, requiresTableScan = false, cartType = type) }
        viewModelScope.launch {
            try {
                state.update { it.copy(cart = cartRepository.getCart(type)) }
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
        runItemAction(item.id, "Đã xóa món khỏi giỏ.") { cartRepository.deleteItem(item.id, cartType()) }
    }

    fun clear() {
        val current = state.value
        if (current.isLoading || current.isClearing || current.busyItemId != null || current.cart.items.isEmpty()) return
        state.update { it.copy(isClearing = true, message = null) }
        viewModelScope.launch {
            try {
                cartRepository.clear(cartType())
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

    fun selectPaymentMethod(paymentMethod: CheckoutPaymentMethod) {
        if (!state.value.isCheckingOut) state.update { it.copy(paymentMethod = paymentMethod) }
    }

    fun checkout() {
        val current = state.value
        if (
            current.cart.items.isEmpty() || current.isLoading || current.isClearing ||
            current.busyItemId != null || current.isCheckingOut
        ) return

        state.update { it.copy(isCheckingOut = true, message = null) }
        viewModelScope.launch {
            try {
                val orderType = when (cartType()) {
                    CartType.DINE_IN -> OrderType.DINE_IN
                    CartType.TAKEAWAY -> OrderType.TAKEAWAY
                    CartType.DELIVERY -> OrderType.DELIVERY
                }
                val paymentMethod = state.value.paymentMethod
                val order = orderRepository.createOrder(orderType, paymentMethod)
                if (paymentMethod == CheckoutPaymentMethod.VNPAY) {
                    state.update { it.copy(paymentUrl = order.paymentUrl) }
                } else {
                    state.update {
                        it.copy(
                            cart = Cart(emptyList(), 0),
                            message = "Đã gửi đơn đến quán. Bạn sẽ thanh toán tiền mặt khi nhận món."
                        )
                    }
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

    /**
     * Return URL chỉ là tín hiệu quay lại app; IPN/backend mới là nguồn sự thật.
     * Poll ngắn để tránh hiển thị thành công khi VNPay đã redirect nhưng IPN đến trễ.
     */
    fun handleVnPayReturn(orderCode: String, gatewayResult: String?) {
        if (orderCode.isBlank() || state.value.isCheckingOut) return
        state.update {
            it.copy(
                isCheckingOut = true,
                message = if (gatewayResult == "failed") {
                    "VNPay chưa hoàn tất. Đang kiểm tra trạng thái giao dịch…"
                } else {
                    "Đang xác nhận thanh toán với máy chủ…"
                }
            )
        }
        viewModelScope.launch {
            try {
                repeat(PAYMENT_POLL_ATTEMPTS) { attempt ->
                    val payment = orderRepository.getVnPayStatus(orderCode)
                    when (payment.paymentStatus) {
                        "PAID" -> {
                            state.update {
                                it.copy(
                                    cart = Cart(emptyList(), 0),
                                    message = "Thanh toán thành công. Đơn đang chờ quán xác nhận."
                                )
                            }
                            return@launch
                        }
                        "FAILED" -> {
                            state.update {
                                it.copy(message = "Thanh toán chưa thành công. Món vẫn được giữ trong giỏ để bạn thử lại hoặc chọn tiền mặt.")
                            }
                            return@launch
                        }
                    }
                    if (!payment.shouldPoll) {
                        state.update { it.copy(message = "Trạng thái giao dịch: ${payment.orderStatus.name}.") }
                        return@launch
                    }
                    if (attempt < PAYMENT_POLL_ATTEMPTS - 1) delay(PAYMENT_POLL_INTERVAL_MS)
                }
                state.update { it.copy(message = "VNPay vẫn đang xử lý. Bạn có thể bấm thanh toán lại để mở lại giao dịch đang chờ.") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            } finally {
                state.update { it.copy(isCheckingOut = false) }
            }
        }
    }

    private fun update(itemId: String, update: CartItemUpdate, successMessage: String) {
        runItemAction(itemId, successMessage) { cartRepository.updateItem(itemId, update, cartType()) }
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
                val cart = cartRepository.getCart(cartType())
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
        if (error is FoodHubApiException && error.statusCode == 401 && tableSessionStore.current() != null) {
            tableSessionStore.clear()
            state.update { it.copy(requiresTableScan = true, error = if (loadError) null else it.error) }
        } else if (error is LoginRequiredException || (error is FoodHubApiException && error.statusCode == 401)) {
            state.update { it.copy(requiresLogin = true, error = if (loadError) null else it.error) }
        } else if (loadError) {
            state.update { it.copy(error = cartError(error)) }
        } else {
            state.update { it.copy(message = cartError(error)) }
        }
    }

    fun dismissLogin() { state.update { it.copy(requiresLogin = false) } }
    fun dismissTableScan() { state.update { it.copy(requiresTableScan = false) } }
    fun consumeMessage() { state.update { it.copy(message = null) } }
    fun consumePaymentUrl() { state.update { it.copy(paymentUrl = null) } }
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

private const val PAYMENT_POLL_ATTEMPTS = 15
private const val PAYMENT_POLL_INTERVAL_MS = 1_500L
