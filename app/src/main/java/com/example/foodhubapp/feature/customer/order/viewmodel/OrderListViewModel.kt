package com.example.foodhubapp.feature.customer.order.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.feature.customer.menu.data.LoginRequiredException
import com.example.foodhubapp.feature.customer.order.data.CustomerOrder
import com.example.foodhubapp.feature.customer.order.data.OrderRepository
import com.example.foodhubapp.feature.customer.order.data.OrderStatus
import com.example.foodhubapp.feature.customer.order.data.OrderType
import com.example.foodhubapp.feature.customer.order.data.OrderItem
import com.example.foodhubapp.feature.customer.order.data.RemoteOrderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException

enum class OrderGroup { ACTIVE, COMPLETED, CANCELLED }

data class OrderListUiState(
    val orders: List<CustomerOrder> = emptyList(),
    val selectedGroup: OrderGroup = OrderGroup.ACTIVE,
    val selectedType: OrderType? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val busyOrderId: String? = null,
    val error: String? = null,
    val message: String? = null,
    val requiresLogin: Boolean = false,
    val paymentUrl: String? = null,
    val page: Int = 0,
    val hasMore: Boolean = false
) {
    val visibleOrders: List<CustomerOrder>
        get() = orders.filter { order ->
            when (selectedGroup) {
                OrderGroup.ACTIVE -> order.status in activeStatuses
                OrderGroup.COMPLETED -> order.status == OrderStatus.COMPLETED
                OrderGroup.CANCELLED -> order.status in cancelledStatuses
            }
        }
}

internal val activeStatuses = setOf(
    OrderStatus.PENDING_PAYMENT,
    OrderStatus.PENDING_CONFIRMATION,
    OrderStatus.CONFIRMED,
    OrderStatus.PREPARING,
    OrderStatus.READY,
    OrderStatus.SERVED,
    OrderStatus.UNKNOWN
)
internal val cancelledStatuses = setOf(OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED)

class OrderListViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: OrderRepository = RemoteOrderRepository(
        accessToken = TokenStore(application.applicationContext)::getAccessToken
    )
) : AndroidViewModel(application) {
    private val tokenStore = TokenStore(application.applicationContext)
    private val state = MutableStateFlow(OrderListUiState())
    val uiState = state.asStateFlow()

    init { load(refresh = true) }

    fun load(refresh: Boolean = false) {
        val current = state.value
        if (current.isRefreshing || current.isLoadingMore || (current.isLoading && !refresh)) return
        state.update {
            it.copy(
                isLoading = !refresh && it.orders.isEmpty(),
                isRefreshing = refresh,
                error = null,
                message = null
            )
        }
        viewModelScope.launch {
            if (tokenStore.getAccessToken().isNullOrBlank()) {
                state.update { it.copy(isLoading = false, isRefreshing = false, requiresLogin = true) }
                return@launch
            }
            try {
                val page = repository.getHistory(page = 1, type = state.value.selectedType)
                state.update {
                    it.copy(orders = page.orders, page = page.page, hasMore = page.hasMore, requiresLogin = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error, loadError = true)
            } finally {
                state.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    fun loadMore() {
        val current = state.value
        if (!current.hasMore || current.isLoading || current.isRefreshing || current.isLoadingMore) return
        state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            if (tokenStore.getAccessToken().isNullOrBlank()) {
                state.update { it.copy(isLoadingMore = false, requiresLogin = true) }
                return@launch
            }
            try {
                val page = repository.getHistory(current.page + 1, type = current.selectedType)
                state.update { old ->
                    old.copy(
                        orders = (old.orders + page.orders).distinctBy { it.id },
                        page = page.page,
                        hasMore = page.hasMore
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            } finally {
                state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun selectGroup(group: OrderGroup) { state.update { it.copy(selectedGroup = group) } }

    fun selectType(type: OrderType?) {
        if (state.value.selectedType == type) return
        state.update { it.copy(selectedType = type, orders = emptyList(), page = 0, hasMore = false, isLoading = false) }
        load()
    }

    fun cancel(order: CustomerOrder, reason: String, onSuccess: () -> Unit = {}) {
        val cleanReason = reason.trim()
        if (cleanReason.isEmpty() || cleanReason.length > 255 || state.value.busyOrderId != null) return
        state.update { it.copy(busyOrderId = order.id, message = null) }
        viewModelScope.launch {
            try {
                repository.cancel(order.id, cleanReason)
                state.update { current ->
                    current.copy(
                        orders = current.orders.map {
                            if (it.id == order.id) it.copy(status = OrderStatus.CANCELLED, cancelReason = cleanReason) else it
                        },
                        message = "Đã hủy đơn ${order.orderCode}."
                    )
                }
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            } finally {
                state.update { it.copy(busyOrderId = null) }
            }
        }
    }

    fun pay(order: CustomerOrder) {
        if (state.value.busyOrderId != null) return
        state.update { it.copy(busyOrderId = order.id, message = null) }
        viewModelScope.launch {
            try {
                val url = repository.createVnPayUrl(order.orderCode)
                state.update { it.copy(paymentUrl = url) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            } finally {
                state.update { it.copy(busyOrderId = null) }
            }
        }
    }

    fun review(order: CustomerOrder, item: OrderItem, rating: Int, comment: String, onSuccess: () -> Unit = {}) {
        if (state.value.busyOrderId != null || rating !in 1..5 || item.menuItemId.isBlank() ||
            comment.trim().length > 1000 || order.status !in setOf(OrderStatus.SERVED, OrderStatus.COMPLETED) ||
            order.items.none { it.menuItemId == item.menuItemId } ||
            item.menuItemId in order.reviewedMenuItemIds) return
        state.update { it.copy(busyOrderId = order.id, message = null) }
        viewModelScope.launch {
            try {
                repository.submitReview(order.id, item.menuItemId, rating, comment)
                state.update { current -> current.copy(
                    orders = current.orders.map {
                        if (it.id == order.id) it.copy(reviewedMenuItemIds = it.reviewedMenuItemIds + item.menuItemId) else it
                    },
                    message = "Cảm ơn bạn đã đánh giá ${item.name}."
                ) }
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            } finally {
                state.update { it.copy(busyOrderId = null) }
            }
        }
    }

    private fun handleError(error: Exception, loadError: Boolean = false) {
        if (error is LoginRequiredException || (error is FoodHubApiException && error.statusCode == 401)) {
            state.update { it.copy(requiresLogin = true, error = null) }
            return
        }
        val message = orderError(error)
        state.update { if (loadError && it.orders.isEmpty()) it.copy(error = message) else it.copy(message = message) }
    }

    fun dismissLogin() { state.update { it.copy(requiresLogin = false) } }
    fun consumeMessage() { state.update { it.copy(message = null) } }
    fun consumePaymentUrl() { state.update { it.copy(paymentUrl = null) } }
}

internal fun orderError(error: Exception): String = when (error) {
    is SocketTimeoutException -> "Máy chủ phản hồi quá lâu. Vui lòng thử lại."
    is IOException -> "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng."
    is FoodHubApiException -> when (error.statusCode) {
        400 -> error.message ?: "Đơn hàng không thể thực hiện thao tác này."
        403 -> "Bạn không có quyền thao tác với đơn hàng này."
        409 -> error.message ?: "Đơn đã thanh toán và cần được hoàn tiền trước."
        422 -> error.message ?: "Dữ liệu gửi lên chưa hợp lệ."
        else -> error.message ?: "Không thể tải đơn hàng."
    }
    else -> error.message ?: "Không thể tải đơn hàng."
}
