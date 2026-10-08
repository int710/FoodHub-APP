package com.example.foodhubapp

import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.order.data.OrderStatus
import com.example.foodhubapp.feature.order.data.OrderType
import com.example.foodhubapp.feature.order.data.CheckoutPaymentMethod
import com.example.foodhubapp.feature.order.data.RemoteOrderRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
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
        assertEquals("/api/v1/order/order%2F1/cancel", cancel.path)
        assertEquals("Đổi món", JSONObject(cancel.body.readUtf8()).getString("reason"))

        assertEquals("https://pay.example/vnpay", repository.createVnPayUrl("FH-9082"))
        val payment = server.takeRequest()
        assertEquals("POST", payment.method)
        assertEquals("/api/v1/payment/vnpay/create", payment.path)
        assertEquals("FH-9082", JSONObject(payment.body.readUtf8()).getString("orderCode"))
    }

    @Test fun createVnPayOrderUsesBackendCheckoutContract() = runBlocking {
        server.enqueue(MockResponse().setBody("""{
            "data": {
                "order": {"id":"order-2","orderCode":"FH-20261008"},
                "orderCode":"FH-20261008",
                "paymentUrl":"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?token=test"
            }
        }"""))

        val created = repository.createVnPayOrder(OrderType.TAKEAWAY, "Ít cay")
        val request = server.takeRequest()
        val body = JSONObject(request.body.readUtf8())

        assertEquals("POST", request.method)
        assertEquals("/api/v1/order/TAKEAWAY/new", request.path)
        assertEquals("Bearer order-token", request.getHeader("Authorization"))
        assertEquals("VNPAY", body.getString("paymentMethod"))
        assertEquals("Ít cay", body.getString("note"))
        assertEquals("order-2", created.orderId)
        assertEquals("FH-20261008", created.orderCode)
        assertTrue(created.paymentUrl.startsWith("https://sandbox.vnpayment.vn/"))
    }

    @Test fun paymentStatusUsesBackendAsSourceOfTruth() = runBlocking {
        server.enqueue(MockResponse().setBody("""{
            "data": {
                "orderId":"order-2",
                "orderCode":"FH/20261008",
                "orderStatus":"PENDING_CONFIRMATION",
                "paymentStatus":"PAID",
                "paidAt":"2026-10-08T15:55:00.000Z",
                "shouldPoll":false
            }
        }"""))

        val status = repository.getVnPayStatus("FH/20261008")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/v1/payment/vnpay/status/FH%2F20261008", request.path)
        assertEquals("Bearer order-token", request.getHeader("Authorization"))
        assertEquals(OrderStatus.PENDING_CONFIRMATION, status.orderStatus)
        assertEquals("PAID", status.paymentStatus)
        assertFalse(status.shouldPoll)
    }

    @Test fun cashCheckoutUsesSameOrderContractWithoutPaymentUrl() = runBlocking {
        server.enqueue(MockResponse().setBody("""{
            "data": {
                "order": {"id":"cash-order","orderCode":"FHUB_CASH_01","status":"PENDING_CONFIRMATION"},
                "items": []
            }
        }"""))

        val created = repository.createOrder(
            type = OrderType.TAKEAWAY,
            paymentMethod = CheckoutPaymentMethod.CASH
        )
        val request = server.takeRequest()
        val body = JSONObject(request.body.readUtf8())

        assertEquals("/api/v1/order/TAKEAWAY/new", request.path)
        assertEquals("CASH", body.getString("paymentMethod"))
        assertEquals("cash-order", created.orderId)
        assertEquals("FHUB_CASH_01", created.orderCode)
        assertEquals(null, created.paymentUrl)
    }

    @Test fun dineInCheckoutUsesTableSessionHeader() = runBlocking {
        val dineInRepository = RemoteOrderRepository(
            FoodHubApiClient(server.url("/api/v1").toString()),
            tableToken = { "table-session-token" }
        )
        server.enqueue(MockResponse().setBody("""{
            "data": {
                "order":{"id":"dine-order","orderCode":"FHUB_DINE_IN"},
                "paymentUrl":"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
            }
        }"""))

        dineInRepository.createVnPayOrder(OrderType.DINE_IN)
        val request = server.takeRequest()

        assertEquals("/api/v1/order/DINE_IN/new", request.path)
        assertEquals("table-session-token", request.getHeader("X-Table-Token"))
    }

    @Test fun lastShortPageHasNoMoreItems() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"data\":[]}"))
        val page = repository.getHistory(2)
        assertFalse(page.hasMore)
    }
}
