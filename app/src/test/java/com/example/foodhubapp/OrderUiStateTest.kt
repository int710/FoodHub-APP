package com.example.foodhubapp

import com.example.foodhubapp.feature.customer.order.data.CustomerOrder
import com.example.foodhubapp.feature.customer.order.data.OrderStatus
import com.example.foodhubapp.feature.customer.order.data.OrderType
import com.example.foodhubapp.feature.customer.order.viewmodel.OrderGroup
import com.example.foodhubapp.feature.customer.order.viewmodel.OrderListUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class OrderUiStateTest {
    @Test fun unpaidWaitingOrdersCanBeCancelledForEveryOrderType() {
        for (type in OrderType.entries) {
            for (status in listOf(OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_CONFIRMATION)) {
                for (payment in listOf("PENDING", "UNPAID", "FAILED")) {
                    assertTrue(order("unpaid", status).copy(type = type, paymentStatus = payment).canCancel)
                }
            }
        }
    }

    @Test fun paidUnknownAndCookingOrdersCannotBeCancelled() {
        for (payment in listOf("PAID", "REFUNDED", null)) {
            for (status in listOf(OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_CONFIRMATION)) {
                assertFalse(order("paid", status).copy(paymentStatus = payment).canCancel)
            }
        }
        for (status in listOf(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED,
            OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.SERVED,
            OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.UNKNOWN)) {
            assertFalse(order("closed", status).copy(paymentStatus = "UNPAID").canCancel)
        }
    }
    private fun order(id: String, status: OrderStatus) = CustomerOrder(
        id, id, null, OrderType.TAKEAWAY, status, "", 0,
        null, null, null, null, null, null, emptyList()
    )

    private val orders = listOf(
        order("active", OrderStatus.PREPARING),
        order("done", OrderStatus.COMPLETED),
        order("cancelled", OrderStatus.CANCELLED),
        order("failed", OrderStatus.PAYMENT_FAILED)
    )

    @Test fun groupsMatchBackendStatuses() {
        assertEquals(listOf("active"), OrderListUiState(orders = orders).visibleOrders.map { it.id })
        assertEquals(
            listOf("done"),
            OrderListUiState(orders = orders, selectedGroup = OrderGroup.COMPLETED).visibleOrders.map { it.id }
        )
        assertEquals(
            listOf("cancelled", "failed"),
            OrderListUiState(orders = orders, selectedGroup = OrderGroup.CANCELLED).visibleOrders.map { it.id }
        )
    }
}
