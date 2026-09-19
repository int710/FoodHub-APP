package com.example.foodhubapp

import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.feature.menu.data.*
import com.example.foodhubapp.feature.menu.ui.FoodCartSelection
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class FoodRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteFoodRepository
    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        repository = RemoteFoodRepository(FoodHubApiClient(server.url("/api/v1").toString())) { "test-token" }
    }
    @After fun tearDown() { server.shutdown() }

    private fun item() = JSONObject("""{
        "id":"burger-id", "name":"Burger", "description":"Fresh burger",
        "basePrice":"99000", "avgRating":"4.2", "image":"https://example.com/burger.png",
        "isAvailable":true, "variantGroups":[{
            "id":"extras", "name":"Extras", "type":"MULTIPLE", "isRequired":false,
            "options":[
                {"id":"egg","name":"Egg","priceAdd":"15000","isActive":true},
                {"id":"inactive","name":"Hidden","priceAdd":10000,"isActive":false}
            ]
        }], "flashSale":null
    }""")

    @Test fun detailUsesActualIdAndFiltersInactiveOptions() = runBlocking {
        server.enqueue(MockResponse().setBody(JSONObject().put("data", item()).toString()))
        val food = repository.getFood("burger-id")
        assertEquals("/api/v1/menu/item/burger-id", server.takeRequest().path)
        assertEquals(99000L, food.basePrice)
        assertEquals("https://example.com/burger.png", food.imageUrl)
        assertEquals(listOf("egg"), food.groups.single().options.map { it.id })
        assertTrue(food.groups.single().multiple)
        assertNull(food.salePrice)
    }

    @Test fun itemWithoutGroupsAndNullFieldsIsSupported() {
        val food = parseFoodDetail(item().put("variantGroups", org.json.JSONArray())
            .put("description", JSONObject.NULL).put("image", JSONObject.NULL))
        assertTrue(food.groups.isEmpty())
        assertEquals("", food.description)
        assertNull(food.imageUrl)
    }

    @Test fun addCartSendsCorrectEndpointBearerAndBody() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"message\":\"Added\",\"data\":{}}"))
        repository.addToCart(FoodCartSelection("burger-id", 2, listOf("egg"), "No onion"))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/cart/TAKEAWAY/items/add", request.path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("burger-id", body.getString("menuItemId"))
        assertEquals(2, body.getInt("quantity"))
        assertEquals("egg", body.getJSONArray("variantOptionIds").getString(0))
        assertEquals("No onion", body.getString("note"))
        assertFalse(body.has("price"))
    }

    @Test fun missingTokenDoesNotSendCartRequest() = runBlocking {
        val guest = RemoteFoodRepository(FoodHubApiClient(server.url("/api/v1").toString()))
        try {
            guest.addToCart(FoodCartSelection("burger-id", 1, emptyList(), ""))
            fail("Expected login requirement")
        } catch (_: LoginRequiredException) { }
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS))
    }

    @Test fun unauthorizedStatusIsPreserved() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"message\":\"Expired token\"}"))
        try {
            repository.addToCart(FoodCartSelection("burger-id", 1, emptyList(), ""))
            fail("Expected unauthorized")
        } catch (error: FoodHubApiException) { assertEquals(401, error.statusCode) }
    }

    @Test fun htmlResponseDoesNotBecomeSuccessfulCartAddition() = runBlocking {
        server.enqueue(MockResponse().setBody("<html>Challenge</html>"))
        try {
            repository.addToCart(FoodCartSelection("burger-id", 1, emptyList(), ""))
            fail("Expected invalid response")
        } catch (error: IllegalStateException) { assertTrue(error.message!!.contains("JSON")) }
    }

    @Test fun menuParsesCategoriesAndActualItemIds() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":[{
            "id":"category", "name":"Burgers", "items":[{
                "id":"burger-id", "name":"Burger", "basePrice":"99000",
                "salePrice":79000, "image":null
            }]
        }]}"""))
        val menu = repository.getMenu()
        assertEquals("burger-id", menu.single().items.single().id)
        assertEquals(79000L, menu.single().items.single().price)
        assertEquals("/api/v1/menu/all", server.takeRequest().path)
    }

    @Test fun flashSaleOnlyAppliesWithinActivePeriod() {
        val data = item().put("flashSale", JSONObject("""{
            "isActive":true, "discountPercent":"20",
            "startsAt":"2026-09-18T00:00:00.000Z", "endsAt":"2026-09-19T00:00:00.000Z"
        }"""))
        assertEquals(79200L, parseFoodDetail(data, 1789732800000L).salePrice)
        assertNull(parseFoodDetail(data, 1789862400000L).salePrice)
    }

    @Test fun loginBodyMatchesEmailApiContract() {
        val body = com.example.foodhubapp.feature.auth.model.LoginRequest(" user@gmail.com ", "password").toJson()
        assertEquals("user@gmail.com", body.getString("email"))
        assertEquals("password", body.getString("password"))
        assertFalse(body.has("account"))
        assertFalse(body.has("emailOrPhone"))
    }
}
