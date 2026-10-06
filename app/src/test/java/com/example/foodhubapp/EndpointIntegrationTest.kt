package com.example.foodhubapp

import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.admin.menu.data.AdminMenuRepository
import com.example.foodhubapp.feature.admin.model.AdminVariantInput
import com.example.foodhubapp.feature.admin.model.AdminVariantOptionInput
import com.example.foodhubapp.feature.admin.model.AdminItemStatus
import com.example.foodhubapp.feature.admin.order.data.AdminOrderRepository
import com.example.foodhubapp.feature.admin.table.data.AdminTableRepository
import com.example.foodhubapp.feature.shared.payment.PaymentRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class EndpointIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var api: FoodHubApiClient
    @Before fun setup() { server = MockWebServer(); server.start(); api = FoodHubApiClient(server.url("/api/v1").toString()) }
    @After fun teardown() { server.shutdown() }

    @Test fun variantEditPreservesIdsAndAddsNewOptionWithoutId() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"variantGroups":[{"id":"group1","name":"Size","type":"SINGLE","sortOrder":3,"isRequired":true,"options":[{"id":"opt1","name":"Nhỏ","priceAdd":"15000"}]}]}}"""))
        server.enqueue(MockResponse().setBody("{}"))
        val repository = AdminMenuRepository(api) { "admin-token" }
        val group = repository.getVariantGroups("food1").single()
        assertEquals(15000L, group.input.options.single().priceAdd)
        assertEquals("/api/v1/menu/item/food1", server.takeRequest().path)
        repository.updateVariant(group.id, group.input.copy(name = "Kích cỡ", multiple = true,
            options = group.input.options.map { it.copy(name = "Vừa", priceAdd = 20000) } + AdminVariantOptionInput("Lớn", 30000)))
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/menu/item-variants/group1", request.path)
        assertEquals("Bearer admin-token", request.getHeader("Authorization"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("MULTIPLE", body.getString("type"))
        assertEquals(3, body.getInt("sortOrder"))
        val options = body.getJSONArray("options")
        assertEquals("opt1", options.getJSONObject(0).getString("id"))
        assertEquals("Vừa", options.getJSONObject(0).getString("name"))
        assertEquals(20000L, options.getJSONObject(0).getLong("priceAdd"))
        assertFalse(options.getJSONObject(1).has("id"))
    }

    @Test fun kitchenParsesItemsAndUpdatesItemRatherThanOrderId() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"line1","orderId":"order1","quantity":2,"status":"WAITING","note":"Ít cay","snapshot":{"name":"Bún bò","variantOptions":[{"id":"option1","name":"Tô lớn"}]},"order":{"id":"order1","type":"DINE_IN","table":{"name":"Bàn 3"}}}],"pagination":{"page":2,"limit":100,"total":101}}"""))
        server.enqueue(MockResponse().setBody("{}"))
        val repository = AdminOrderRepository(api) { "staff-token" }
        val item = repository.getKitchenItems(2).single()
        assertEquals("Bún bò", item.name)
        assertEquals("Bàn 3", item.destination)
        assertEquals(2, item.quantity)
        assertEquals("Ít cay", item.note)
        assertEquals(listOf("Tô lớn"), item.options)
        val get = server.takeRequest()
        assertEquals("/api/v1/order/kitchen?page=2&limit=100", get.path)
        assertEquals("Bearer staff-token", get.getHeader("Authorization"))
        repository.updateItem(item.id, AdminItemStatus.PREPARING)
        val patch = server.takeRequest()
        assertEquals("/api/v1/order/kitchen/line1/status", patch.path)
        assertEquals("PREPARING", JSONObject(patch.body.readUtf8()).getString("status"))
    }

    @Test fun tableDetailUsesFreshBackendData() = runBlocking {
        val table = """{"id":"table1","name":"Bàn 1","capacity":6,"floor":"Tầng 2","isActive":false,"note":"Cạnh cửa"}"""
        server.enqueue(MockResponse().setBody("{\"data\":[$table]}"))
        server.enqueue(MockResponse().setBody("{\"data\":$table}"))
        val repository = AdminTableRepository(api) { "admin-token" }
        assertEquals(1, repository.getTables().size)
        assertEquals("/api/v1/table", server.takeRequest().path)
        val detail = repository.getTable("table1")
        assertEquals("/api/v1/table/table1", server.takeRequest().path)
        assertEquals(6, detail.capacity)
        assertFalse(detail.isActive)
        assertEquals("Cạnh cửa", detail.note)
    }

    @Test fun paymentDetailsRequireAccountAndHandleNoPayment() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"orderId":"order1","method":"VNPAY","status":"PAID","amount":"45000","paidAt":"2026-10-06T09:00:00Z"}}"""))
        server.enqueue(MockResponse().setBody("{\"data\":null}"))
        val repository = PaymentRepository(api) { "customer-token" }
        val detail = repository.getDetail("order1")!!
        assertEquals("PAID", detail.getString("status"))
        assertEquals("45000", detail.getString("amount"))
        val request = server.takeRequest()
        assertEquals("/api/v1/payment/order1", request.path)
        assertEquals("Bearer customer-token", request.getHeader("Authorization"))
        assertNull(repository.getDetail("order2"))
        server.takeRequest()
        try {
            PaymentRepository(api) { null }.getDetail("order3")
            fail("Missing session must not send the request")
        } catch (_: IllegalStateException) { assertEquals(2, server.requestCount) }
    }

    @Test fun failedVariantSavePropagatesServerError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400).setBody("{\"message\":\"Nhóm không tồn tại\"}"))
        try {
            AdminMenuRepository(api) { "admin-token" }.updateVariant("missing", AdminVariantInput("Size", false, true, listOf(AdminVariantOptionInput("Lớn", 10000))))
            fail("Must report rejected save")
        } catch (e: Exception) { assertTrue(e.message.orEmpty().contains("Nhóm không tồn tại")) }
        server.takeRequest()
        Unit
    }
}
