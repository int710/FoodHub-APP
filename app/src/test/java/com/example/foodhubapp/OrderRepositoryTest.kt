package com.example.foodhubapp

import com.example.foodhubapp.core.network.*
import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.customer.order.data.OrderStatus
import com.example.foodhubapp.feature.customer.order.data.OrderType
import com.example.foodhubapp.feature.customer.order.data.RemoteOrderRepository
import com.example.foodhubapp.feature.customer.menu.data.LoginRequiredException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrderRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteOrderRepository

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        repository = RemoteOrderRepository(FoodHubApiClient(server.url("/api/v1").toString())) { "order-token" }
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun historyParsesNestedOrderAndPagination() = runBlocking {
        server.enqueue(MockResponse().setBody("""{
            "data": {"orders": [{
                "id": "order-1",
                "orderCode": "FH-9082",
                "type": "DINE_IN",
                "status": "PENDING_PAYMENT",
                "createdAt": "2026-09-19T12:20:00Z",
                "totalAmount": "358000",
                "table": {"name":"Bàn 05","floor":"Tầng 1"},
                "payments": [{"method":"VNPAY","status":"PENDING"}],
                "items": [{
                    "id":"line-1","quantity":1,"unitPrice":"145000","subTotal":"145000",
                    "snapshot":{"name":"Truffle Smash Burger","image":"https://example.com/burger.jpg","options":[{"name":"Sốt cay nhẹ"}]}
                }]
            }]},
            "pagination": {"page":1,"totalPages":2,"total":21}
        }"""))

        val page = repository.getHistory(1, type = OrderType.DINE_IN)
        val request = server.takeRequest()

        assertEquals("/api/v1/order/history?page=1&limit=20&type=DINE_IN", request.path)
        assertEquals("Bearer order-token", request.getHeader("Authorization"))
        assertTrue(page.hasMore)
        assertEquals(OrderStatus.PENDING_PAYMENT, page.orders.single().status)
        assertTrue(page.orders.single().canCancel)
        assertEquals("Bàn 05", page.orders.single().tableName)
        assertEquals("Truffle Smash Burger", page.orders.single().items.single().name)
        assertEquals("Sốt cay nhẹ", page.orders.single().items.single().description)
    }

    @Test fun cancelAndPaymentUseDocumentedBodies() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"message\":\"Cancelled\"}"))
        server.enqueue(MockResponse().setBody("{\"data\":{\"paymentUrl\":\"https://pay.example/vnpay\"}}"))

        repository.cancel("order/1", "Đổi món")
        val cancel = server.takeRequest()
        assertEquals("PATCH", cancel.method)
        assertEquals("Bearer order-token", cancel.getHeader("Authorization"))
        assertEquals("/api/v1/order/order%2F1/cancel", cancel.path)
        assertEquals("Đổi món", parseJsonObject(cancel.body.readUtf8()).getString("reason"))

        assertEquals("https://pay.example/vnpay", repository.createVnPayUrl("FH-9082"))
        val payment = server.takeRequest()
        assertEquals("POST", payment.method)
        assertEquals("/api/v1/payment/vnpay/create", payment.path)
        assertEquals("FH-9082", parseJsonObject(payment.body.readUtf8()).getString("orderCode"))
    }

    @Test fun lastShortPageHasNoMoreItems() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"data\":[]}"))
        val page = repository.getHistory(2)
        assertFalse(page.hasMore)
    }

    @Test fun guestHistoryUsesTableSessionWithoutBearerToken() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"data\":[]}"))
        val guest = RemoteOrderRepository(
            apiClient = FoodHubApiClient(server.url("/api/v1/").toString()),
            tableToken = { "table-session" },
        )
        guest.getHistory(2, type = OrderType.DINE_IN)
        val request = server.takeRequest()
        assertEquals("/api/v1/order/table-history?page=2&limit=20&type=DINE_IN", request.path)
        assertEquals("table-session", request.getHeader("X-Table-Token"))
        assertEquals(null, request.getHeader("Authorization"))
    }

    @Test fun loggedInHistoryPrefersBearerOverTableSession() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"data\":[]}"))
        val loggedIn = RemoteOrderRepository(
            apiClient = FoodHubApiClient(server.url("/api/v1/").toString()),
            tableToken = { "table-session" },
            accessToken = { "user-token" },
        )
        loggedIn.getHistory(1)
        val request = server.takeRequest()
        assertEquals("/api/v1/order/history?page=1&limit=20", request.path)
        assertEquals("Bearer user-token", request.getHeader("Authorization"))
        assertEquals(null, request.getHeader("X-Table-Token"))
    }

    @Test fun guestWithoutSessionCannotLoadHistoryOrCancel() = runBlocking {
        val guest = RemoteOrderRepository(
            apiClient = FoodHubApiClient(server.url("/api/v1/").toString()),
        )
        assertTrue(runCatching { guest.getHistory(1) }.exceptionOrNull() is LoginRequiredException)
        val tableGuest = RemoteOrderRepository(
            apiClient = FoodHubApiClient(server.url("/api/v1/").toString()),
            tableToken = { "table-session" },
        )
        assertTrue(runCatching { tableGuest.cancel("order-1", "Đổi món") }.exceptionOrNull() is LoginRequiredException)
        assertEquals(0, server.requestCount)
    }
}
