package com.example.foodhubapp

import com.example.foodhubapp.core.network.*
import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.customer.cart.data.CartItemUpdate
import com.example.foodhubapp.feature.customer.cart.data.RemoteCartRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import com.example.foodhubapp.core.network.FoodHubApiException
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
        val body = parseJsonObject(request.body.readUtf8())

        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/cart/TAKEAWAY/items/cart+line%2F1", request.path)
        assertEquals(3, body.getInt("quantity"))
        assertEquals(2, body.getArray("variantOptionIds").size())
        assertEquals("Ít sốt", body.getString("note"))
        assertFalse(body.has("menuItemId"))
    }

    @Test fun rawCartLoadsMenuDetailsAndSelectedOptionsOncePerMenuItem() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"items":[
            {"id":"line-1","menuItemId":"burger-id","quantity":2,"variantOptionIds":["egg"],"note":"Không hành"},
            {"id":"line-2","menuItemId":"burger-id","quantity":1,"variantOptionIds":[],"note":""}
        ]}}"""))
        server.enqueue(MockResponse().setBody("""{"data":{
            "id":"burger-id","name":"Burger","image":"https://example.com/burger.png",
            "basePrice":"99000","isAvailable":true,
            "variantGroups":[{"id":"topping","name":"Topping","type":"MULTIPLE","isRequired":false,
                "options":[{"id":"egg","name":"Trứng","priceAdd":"15000","isActive":true}]}]
        }}"""))

        val cart = repository.getCart()

        assertEquals("/api/v1/cart/TAKEAWAY/items", server.takeRequest().path)
        assertEquals("/api/v1/menu/item/burger-id", server.takeRequest().path)
        assertEquals(2, server.requestCount)
        val first = cart.items.first()
        assertEquals("line-1", first.id)
        assertEquals("Burger", first.name)
        assertEquals("https://example.com/burger.png", first.imageUrl)
        assertEquals("Không hành", first.note)
        assertEquals("Trứng", first.options.single().name)
        assertEquals(114000L, first.unitPrice)
        assertEquals(228000L, first.subTotal)
        assertEquals(99000L, cart.items.last().subTotal)
        assertEquals(327000L, cart.totalAmount)
    }

    @Test fun pricedCartPreservesServerTotalAndDoesNotAddOptionsTwice() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{
            "items":[{"id":"line-1","menuItemId":"burger-id","name":"Burger",
                "imageUrl":"https://example.com/burger.png","quantity":2,"unitPrice":114000,
                "variantOptions":[{"id":"egg","name":"Trứng","priceAdd":15000}]}],
            "totalAmount":250800
        }}"""))

        val cart = repository.getCart()

        assertEquals(1, server.requestCount)
        assertEquals(228000L, cart.items.single().subTotal)
        assertEquals(250800L, cart.totalAmount)
    }

    @Test fun missingMenuDetailsReturnAnErrorInsteadOfAnUnnamedZeroPriceItem() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"items":[
            {"id":"line-1","menuItemId":"deleted-item","quantity":1}
        ]}}"""))
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Món không còn tồn tại"}"""))
        try {
            repository.getCart()
            fail("Expected menu lookup failure")
        } catch (error: FoodHubApiException) {
            assertEquals(404, error.statusCode)
        }
    }

    @Test fun deleteItemAndClearUseDocumentedEndpoints() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        repository.deleteItem("line-id")
        repository.clear()

        assertEquals("DELETE /api/v1/cart/TAKEAWAY/items/line-id", server.takeRequest().let { "${it.method} ${it.path}" })
        assertEquals("DELETE /api/v1/cart/TAKEAWAY/clear", server.takeRequest().let { "${it.method} ${it.path}" })
    }
}
