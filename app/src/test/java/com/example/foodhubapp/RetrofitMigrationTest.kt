package com.example.foodhubapp

import com.example.foodhubapp.core.network.*
import com.example.foodhubapp.feature.admin.chat.data.AdminConversationRepository
import com.example.foodhubapp.feature.admin.menu.data.AdminMenuRepository
import com.example.foodhubapp.feature.admin.model.AdminMenuInput
import com.example.foodhubapp.feature.admin.order.data.AdminOrderRepository
import com.example.foodhubapp.feature.admin.table.data.AdminTableRepository
import com.example.foodhubapp.feature.customer.cart.data.CartType
import com.example.foodhubapp.feature.customer.cart.data.CheckoutRequest
import com.example.foodhubapp.feature.customer.cart.data.RemoteCartRepository
import com.example.foodhubapp.feature.customer.order.data.CheckoutPaymentMethod
import com.example.foodhubapp.feature.customer.order.data.RemoteOrderRepository
import com.example.foodhubapp.feature.shared.notification.data.NotificationRepository
import com.example.foodhubapp.feature.shared.auth.model.AuthResponse
import com.example.foodhubapp.feature.shared.auth.model.LoginRequest
import com.example.foodhubapp.feature.shared.auth.model.RegisterRequest
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RetrofitMigrationTest {
    private lateinit var server: MockWebServer
    private lateinit var api: FoodHubApiClient
    private val headers = mapOf("Authorization" to "Bearer admin-token")

    @Before fun setup() {
        server = MockWebServer().apply { start() }
        api = FoodHubApiClient(server.url("/api/v1/").toString())
    }

    @After fun teardown() { server.shutdown() }

    @Test fun checkoutKeepsAmountAndPaymentQrFromLatestMain() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"order":{"id":"o1","orderCode":"FH-123","totalAmount":"125000"},"paymentUrl":"https://pay.example/qr","qrCode":"qr-content"}}"""))
        val repository = RemoteCartRepository(apiClient = api, accessToken = { "customer-token" })
        val result = repository.checkout(CheckoutRequest(
            type = CartType.TAKEAWAY,
            paymentMethod = "ZALOPAY",
        ))
        assertEquals(125000L, result.amount)
        assertEquals("FH-123", result.orderCode)
        assertEquals("https://pay.example/qr", result.paymentUrl)
        assertEquals("qr-content", result.qrContent)
        assertEquals("/api/v1/order/TAKEAWAY/new", server.takeRequest().path)
    }

    @Test fun paymentPollingRemainsPublicAsOnLatestMain() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"orderId":"o1","orderCode":"FH-123","orderStatus":"PENDING_PAYMENT","paymentStatus":"PENDING","shouldPoll":true}}"""))
        val repository = RemoteOrderRepository(apiClient = api)
        val result = repository.getPaymentStatus("FH-123", CheckoutPaymentMethod.ZALOPAY)
        assertTrue(result.shouldPoll)
        assertEquals("PENDING", result.paymentStatus)
        val request = server.takeRequest()
        assertEquals("/api/v1/payment/zalopay/status/FH-123", request.path)
        assertNull(request.getHeader("Authorization"))
    }

    @Test fun menuUpdatePreservesExplicitNullsAndNumericPrices() = runBlocking {
        server.enqueue(MockResponse().setBody("{}"))
        AdminMenuRepository(api) { "admin-token" }.updateItem(
            "food1", AdminMenuInput(
                categoryId = "category1", name = " Món mới ", basePrice = 45000,
                description = null, imageUrl = null,
                isAvailable = true, isFeatured = false, sortOrder = 0
            )
        )
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/menu/items/food1", request.path)
        assertEquals("Bearer admin-token", request.getHeader("Authorization"))
        val body = parseJsonObject(request.body.readUtf8())
        assertEquals("Món mới", body.getString("name"))
        assertTrue(body.has("description") && body.isNull("description"))
        assertTrue(body.has("image") && body.isNull("image"))
        assertTrue(body.getAsJsonPrimitive("basePrice").isNumber)
        assertEquals(45000L, body.getLong("basePrice"))
    }

    @Test fun adminMenuRetainsNestedPaginationAndStringMoney() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"data":[{
            "id":"food1","name":"Burger","basePrice":"99000",
            "category":{"id":"cat1","name":"Burger"},
            "flashSale":{"isActive":true,"discountPercent":10}
        }]}}"""))
        val item = AdminMenuRepository(api) { "admin-token" }.getItems().single()
        assertEquals(99000L, item.price)
        assertEquals(89100L, item.salePrice)
        assertEquals("cat1", item.categoryId)
        assertEquals("/api/v1/menu/items?page=1&limit=100", server.takeRequest().path)
    }

    @Test fun adminOrderActionsKeepTheirMethodsPathsAndEmptyBodies() = runBlocking {
        val repository = AdminOrderRepository(api) { "admin-token" }
        val actions: List<Pair<String, suspend () -> Unit>> = listOf(
            "order/o1/confirm" to { repository.confirm("o1") },
            "order/o1/serve" to { repository.serve("o1") },
            "order/o1/complete" to { repository.complete("o1") },
            "payment/o1/cash-confirm" to { repository.confirmCash("o1") }
        )
        for ((path, action) in actions) {
            server.enqueue(MockResponse().setResponseCode(204))
            action()
            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/$path", request.path)
            assertEquals("{}", request.body.readUtf8())
            assertEquals("Bearer admin-token", request.getHeader("Authorization"))
        }
        server.enqueue(MockResponse().setBody("{}"))
        repository.reject("o1", " Hết món ")
        assertEquals("Hết món", parseJsonObject(server.takeRequest().body.readUtf8()).getString("reason"))
    }

    @Test fun tableCreateKeepsNestedTableResponseAndOptionalNulls() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"table":{"id":"t1","name":"Bàn 1","capacity":4,"isActive":true}}}"""))
        val table = AdminTableRepository(api) { "admin-token" }.createTable(" Bàn 1 ", 4, null, null)
        assertEquals("t1", table.id)
        assertEquals(4, table.capacity)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/table/new", request.path)
        val body = parseJsonObject(request.body.readUtf8())
        assertTrue(body.has("floor") && body.isNull("floor"))
        assertTrue(body.has("note") && body.isNull("note"))
    }

    @Test fun tableQrSupportsStringAndObjectPayloads() = runBlocking {
        val repository = AdminTableRepository(api) { "admin-token" }
        server.enqueue(MockResponse().setBody("""{"data":"qr-token"}"""))
        assertEquals("qr-token", repository.getQrContent("t1"))
        assertEquals("/api/v1/table/t1/qr", server.takeRequest().path)
        server.enqueue(MockResponse().setBody("""{"data":{"qrToken":"new-token"}}"""))
        assertEquals("new-token", repository.getQrContent("t1"))
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/table/t1/qr", request.path)
    }

    @Test fun conversationsKeepMainDisplayFieldsAndCloseAccepts204() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":[{"_id":"c1","customerName":"An","lastMessage":"Hello","lastMessageSenderId":"customer1","assignedHostId":"admin1","orderCode":"FH-123"}]}"""))
        val repository = AdminConversationRepository(api) { "admin-token" }
        val conversation = repository.getOpenConversations().single()
        assertEquals("c1", conversation.id)
        assertEquals(1, conversation.unread)
        assertTrue(conversation.isOnline)
        assertEquals("FH-123", conversation.orderCode)
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(204))
        repository.close("c1")
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/conversations/c1/close", request.path)
        assertEquals("{}", request.body.readUtf8())
    }

    @Test fun notificationsKeepQueryUnreadCountAndMarkReadEndpoints() = runBlocking {
        val repository = NotificationRepository(api) { "customer-token" }
        server.enqueue(MockResponse().setBody("""{"data":[{"_id":"n1","title":"Đơn hàng","readAt":null}]}"""))
        assertFalse(repository.getNotifications(true).single().isRead)
        assertEquals("/api/v1/notifications?unreadOnly=true&page=1&limit=50", server.takeRequest().path)
        server.enqueue(MockResponse().setBody("""{"data":{"count":4}}"""))
        assertEquals(4, repository.getUnreadCount())
        server.takeRequest()
        for (id in listOf("n1", null)) {
            server.enqueue(MockResponse().setResponseCode(204))
            if (id == null) repository.markAllAsRead() else repository.markAsRead(id)
            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals(if (id == null) "/api/v1/notifications/read-all" else "/api/v1/notifications/n1/read", request.path)
        }
    }

    @Test fun uploadUsesImagePartAndRefreshesExpiredAuthorization() {
        var refreshCount = 0
        val client = FoodHubApiClient(server.url("/api/v1/").toString(), OkHttpClient()) { _, _, rejected ->
            assertEquals("admin-token", rejected)
            refreshCount++
            "new-token"
        }
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Expired"}"""))
        server.enqueue(MockResponse().setBody("""{"data":[{"url":"https://example.com/image.png"}]}"""))
        val response = client.uploadImage("/media/upload-image", "food.png", "image/png", byteArrayOf(1, 2, 3), headers)
        assertEquals("https://example.com/image.png", response.getArray("data").getObject(0).getString("url"))
        assertEquals(1, refreshCount)
        server.takeRequest()
        val retry = server.takeRequest()
        assertEquals("Bearer new-token", retry.getHeader("Authorization"))
        assertEquals("/api/v1/media/upload-image", retry.path)
        assertTrue(retry.getHeader("Content-Type").orEmpty().startsWith("multipart/form-data"))
        assertTrue(retry.body.readUtf8().contains("name=\"image\"; filename=\"food.png\""))
    }

    @Test fun retryStopsAfterOneRefreshWhenServerStillReturns401() {
        var refreshCount = 0
        val client = FoodHubApiClient(server.url("/api/v1").toString(), OkHttpClient()) { _, _, _ ->
            refreshCount++; "new-token"
        }
        repeat(2) { server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Denied"}""")) }
        val error = assertThrows(FoodHubApiException::class.java) {
            client.execute(client.authApi.getProfile(headers))
        }
        assertEquals(401, error.statusCode)
        assertEquals(1, refreshCount)
        assertEquals(2, server.requestCount)
    }

    @Test fun authEndpointsAndTableOnlyRequestsDoNotRefreshAccountTokens() {
        var refreshCount = 0
        val client = FoodHubApiClient(server.url("/api/v1/").toString(), OkHttpClient()) { _, _, _ ->
            refreshCount++; "new-token"
        }
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Sai mật khẩu"}"""))
        assertThrows(FoodHubApiException::class.java) {
            client.execute(client.authApi.login(headers, JsonObject().put("password", "wrong")))
        }
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Phiên bàn đã hết hạn"}"""))
        assertThrows(FoodHubApiException::class.java) {
            client.execute(client.cartApi.getItems(mapOf("x-table-token" to "table-token"), "DINE_IN"))
        }
        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
        assertEquals("table-token", request.getHeader("x-table-token"))
        assertEquals(0, refreshCount)
    }

    @Test fun backendValidationHtmlAndEmptyBodiesKeepExistingBehavior() {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"errors":{"name":"Tên bắt buộc","phone":"Số điện thoại sai"}}"""))
        val validation = assertThrows(FoodHubApiException::class.java) {
            api.execute(api.authApi.updateProfile(headers, JsonObject()))
        }
        assertEquals("Tên bắt buộc\nSố điện thoại sai", validation.message)
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Unavailable</html>"))
        val html = assertThrows(FoodHubApiException::class.java) { api.execute(api.menuApi.getMenu()) }
        assertEquals(502, html.statusCode)
        assertTrue(html.message.orEmpty().contains("Máy chủ đang chặn"))
        server.enqueue(MockResponse().setBody(""))
        assertEquals(0, api.execute(api.menuApi.getMenu()).size())
        server.enqueue(MockResponse().setResponseCode(204))
        assertEquals(0, api.execute(api.conversationApi.closeConversation(headers = headers, id = "c1", body = JsonObject())).size())
    }

    @Test fun malformedSuccessfulJsonIsRejected() {
        server.enqueue(MockResponse().setBody("<html>Not JSON</html>"))
        val error = assertThrows(IllegalStateException::class.java) { api.execute(api.menuApi.getMenu()) }
        assertTrue(error.message.orEmpty().contains("Máy chủ chưa trả dữ liệu JSON"))
    }

    @Test fun scanAndEndSessionUseTableTokenWithoutAccountAuthorization() {
        server.enqueue(MockResponse().setBody("""{"data":{"tableToken":"table-token"}}"""))
        api.execute(api.tableApi.scan(body = JsonObject().put("qrToken", "qr-value")))
        val scan = server.takeRequest()
        assertEquals("/api/v1/table/scan", scan.path)
        assertEquals("qr-value", parseJsonObject(scan.body.readUtf8()).getString("qrToken"))
        assertNull(scan.getHeader("Authorization"))
        server.enqueue(MockResponse().setResponseCode(204))
        api.execute(api.tableApi.endSession(mapOf("X-Table-Token" to "table-token")))
        val end = server.takeRequest()
        assertEquals("/api/v1/table/session/end", end.path)
        assertEquals("table-token", end.getHeader("X-Table-Token"))
        assertNull(end.getHeader("Authorization"))
    }

    @Test fun authRequestsAndNestedTokenAliasesKeepTheirWireFormat() {
        server.enqueue(MockResponse().setBody("""{"data":{"auth":{"access_token":"access","refresh_token":"refresh"},"user":{"_id":"u1","name":"An","phone":"0123","role":"ADMIN"}}}"""))
        val login = LoginRequest(" user@example.com ", " mật khẩu ")
        val response = AuthResponse.fromJson(api.execute(api.authApi.login(body = login.toJson())))
        assertEquals("access", response.accessToken)
        assertEquals("refresh", response.refreshToken)
        assertEquals("u1", response.user?.id)
        assertEquals("ADMIN", response.user?.role)
        val request = server.takeRequest()
        assertEquals("/api/v1/user/login", request.path)
        assertNull(request.getHeader("Authorization"))
        val body = parseJsonObject(request.body.readUtf8())
        assertEquals("user@example.com", body.getString("email"))
        assertEquals(" mật khẩu ", body.getString("password"))
        server.enqueue(MockResponse().setBody("{}"))
        val register = RegisterRequest("An", "0123", "user@example.com", "password", "password")
        api.execute(api.authApi.register(body = register.toJson()))
        val registration = parseJsonObject(server.takeRequest().body.readUtf8())
        assertEquals("An", registration.getString("name"))
        assertEquals("0123", registration.getString("phone"))
        assertEquals("password", registration.getString("confirmPassword"))
        assertFalse(registration.has("fullName"))
    }
}
