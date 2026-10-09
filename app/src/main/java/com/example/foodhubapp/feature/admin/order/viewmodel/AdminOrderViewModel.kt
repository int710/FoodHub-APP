package com.example.foodhubapp.feature.admin.order.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.feature.admin.model.AdminItemStatus
import com.example.foodhubapp.feature.admin.model.AdminOrder
import com.example.foodhubapp.feature.admin.model.AdminZaloPayment
import com.example.foodhubapp.feature.admin.order.data.AdminOrderRepository
import com.example.foodhubapp.feature.admin.order.data.AdminOrderSocketEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminOrderUiState(
    val orders: List<AdminOrder> = emptyList(),
    val isLoading: Boolean = true,
    val busyId: String? = null,
    val error: String? = null,
    val message: String? = null,
    val zaloPayment: AdminZaloPayment? = null,
)

class AdminOrderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminOrderRepository(application.applicationContext)
    private val state = MutableStateFlow(AdminOrderUiState())
    val uiState = state.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (state.value.busyId != null) return
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                state.update { it.copy(orders = repository.getOrders(), isLoading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(isLoading = false, error = error.message ?: "Không thể tải đơn hàng") }
            }
        }
    }

    fun onRealtimeEvent(event: AdminOrderSocketEvent) {
        val notice = when (event.name) {
            "order:new" -> "Có đơn hàng mới. Danh sách đã được cập nhật."
            "order:item:update" -> "Trạng thái món vừa thay đổi."
            else -> "Đơn hàng vừa chuyển sang ${event.status.orEmpty().replace('_', ' ')}."
        }
        viewModelScope.launch {
            runCatching { repository.getOrders() }
                .onSuccess { orders -> state.update { it.copy(orders = orders, isLoading = false, message = notice) } }
                .onFailure { error -> state.update { it.copy(error = error.message ?: "Không thể đồng bộ đơn hàng") } }
        }
    }

    fun confirm(orderId: String) = run(orderId, "Đã xác nhận đơn") { repository.confirm(orderId) }
    fun reject(orderId: String, reason: String) = run(orderId, "Đã từ chối đơn") { repository.reject(orderId, reason) }
    fun serve(orderId: String) = run(orderId, "Đã phục vụ đơn") { repository.serve(orderId) }
    fun complete(orderId: String) = run(orderId, "Đã hoàn tất đơn") { repository.complete(orderId) }
    fun confirmCash(orderId: String) = run(orderId, "Đã xác nhận thanh toán tiền mặt") { repository.confirmCash(orderId) }
    fun convertCashToZaloPay(orderId: String) {
        if (state.value.busyId != null) return
        state.update { it.copy(busyId = orderId, error = null, message = null) }
        viewModelScope.launch {
            try {
                val payment = repository.convertCashToZaloPay(orderId)
                val orders = repository.getOrders()
                state.update {
                    it.copy(
                        orders = orders,
                        busyId = null,
                        zaloPayment = payment,
                        message = "Đã chuyển đơn sang ZaloPay. Đưa QR cho khách quét.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(busyId = null, error = error.message ?: "Không thể tạo QR ZaloPay") }
            }
        }
    }
    fun updateItem(itemId: String, status: AdminItemStatus) = run(itemId, "Đã cập nhật món") { repository.updateItem(itemId, status) }

    private fun run(id: String, message: String, action: suspend () -> Unit) {
        if (state.value.busyId != null) return
        state.update { it.copy(busyId = id, error = null, message = null) }
        viewModelScope.launch {
            try {
                action()
                val orders = repository.getOrders()
                state.update { it.copy(orders = orders, busyId = null, message = message) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(busyId = null, error = error.message ?: "Không thể cập nhật đơn") }
            }
        }
    }

    fun consumeMessage() { state.update { it.copy(message = null) } }
    fun consumeZaloPayment() { state.update { it.copy(zaloPayment = null) } }
}
