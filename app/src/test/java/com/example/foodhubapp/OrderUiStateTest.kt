package com.example.foodhubapp

import com.example.foodhubapp.feature.order.data.CustomerOrder
import com.example.foodhubapp.feature.order.data.OrderStatus
import com.example.foodhubapp.feature.order.data.OrderType
import com.example.foodhubapp.feature.order.viewmodel.OrderGroup
import com.example.foodhubapp.feature.order.viewmodel.OrderListUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderUiStateTest {
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
