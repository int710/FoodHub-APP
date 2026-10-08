package com.example.foodhubapp

import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.cart.data.CartItemUpdate
import com.example.foodhubapp.feature.cart.data.CartType
import com.example.foodhubapp.feature.cart.data.RemoteCartRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class CartRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteCartRepository

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        repository = RemoteCartRepository(FoodHubApiClient(server.url("/api/v1").toString())) { "cart-token" }
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun getCartParsesItemsAndUsesBearerToken() = runBlocking {
        server.enqueue(MockResponse().setBody("""{
            "data": {
                "items": [{
                    "id": "cart-line-1",
                    "menuItemId": "burger-id",
                    "name": "Burger",
                    "image": "https://example.com/burger.png",
                    "quantity": 2,
                    "unitPrice": "99000",
                    "subTotal": 228000,
                    "variantOptions": [{"id":"egg","name":"Trứng","priceAdd":15000}],
                    "note": "Không hành"
                }],
                "totalAmount": "228000"
            }
        }"""))

        val cart = repository.getCart()
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/v1/cart/TAKEAWAY/items", request.path)
        assertEquals("Bearer cart-token", request.getHeader("Authorization"))
        assertEquals(228000L, cart.totalAmount)
        assertEquals("cart-line-1", cart.items.single().id)
        assertEquals("burger-id", cart.items.single().menuItemId)
        assertEquals("egg", cart.items.single().options.single().id)
    }

    @Test fun updateCartSendsOnlyDocumentedFields() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"message\":\"Updated\"}"))

        repository.updateItem(
            "cart line/1",
            CartItemUpdate(3, listOf("egg", "cheese", "egg"), "Ít sốt")
        )
        val request = server.takeRequest()
        val body = JSONObject(request.body.readUtf8())

        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/cart/TAKEAWAY/items/cart+line%2F1", request.path)
        assertEquals(3, body.getInt("quantity"))
        assertEquals(2, body.getJSONArray("variantOptionIds").length())
        assertEquals("Ít sốt", body.getString("note"))
        assertFalse(body.has("menuItemId"))
    }

    @Test fun deleteItemAndClearUseDocumentedEndpoints() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        repository.deleteItem("line-id")
        repository.clear()

        assertEquals("DELETE /api/v1/cart/TAKEAWAY/items/line-id", server.takeRequest().let { "${it.method} ${it.path}" })
        assertEquals("DELETE /api/v1/cart/TAKEAWAY/clear", server.takeRequest().let { "${it.method} ${it.path}" })
    }

    @Test fun dineInCartUsesTableSessionHeader() = runBlocking {
        val dineInRepository = RemoteCartRepository(
            FoodHubApiClient(server.url("/api/v1").toString()),
            tableToken = { "table-session-token" }
        )
        server.enqueue(MockResponse().setBody("{\"data\":{\"items\":[],\"totalAmount\":0}}"))

        dineInRepository.getCart(CartType.DINE_IN)
        val request = server.takeRequest()

        assertEquals("/api/v1/cart/DINE_IN/items", request.path)
        assertEquals("table-session-token", request.getHeader("X-Table-Token"))
    }
}
