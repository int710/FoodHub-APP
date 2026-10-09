package com.example.foodhubapp

import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.feature.customer.menu.data.RemoteFoodRepository
import com.example.foodhubapp.feature.customer.order.data.RemoteOrderRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class ReviewRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: FoodHubApiClient
    @Before fun setup() { server = MockWebServer(); server.start(); api = FoodHubApiClient(server.url("/api/v1").toString()) }
    @After fun teardown() { server.shutdown() }

    @Test fun readsActualBackendReviewResponse() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"r1","rating":4,"comment":"Ngon","customer":{"name":"An"}}],"total":1,"avg":{"avgRating":4,"count":1}}"""))
        val review = RemoteFoodRepository(api).getReviews("food1").single()
        assertEquals("An", review.customerName)
        assertEquals(4, review.rating)
        assertEquals("Ngon", review.comment)
        assertEquals("/api/v1/reviews/items/food1?page=1&limit=20", server.takeRequest().path)
    }

    @Test fun feedbackUsesAuthAndValidatesInputBeforeRequest() = runBlocking {
        val repository = RemoteOrderRepository(api) { "token" }
        server.enqueue(MockResponse().setBody("{}"))
        repository.submitReview("order1", "food1", 5, " Ngon ")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/reviews/feedback", request.path)
        assertEquals("Bearer token", request.getHeader("Authorization"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("order1", body.getString("orderId"))
        assertEquals("food1", body.getString("menuItemId"))
        assertEquals(5, body.getInt("rating"))
        assertEquals("Ngon", body.getString("comment"))
        for (rating in listOf(0, 6)) {
            try { repository.submitReview("order1", "food1", rating, ""); fail() }
            catch (_: IllegalArgumentException) { }
        }
        assertEquals(1, server.requestCount)
    }

    @Test fun historyRestoresReviewedItemsAndFeedbackPropagatesFailure() = runBlocking {
        val repository = RemoteOrderRepository(api) { "token" }
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"order1","status":"COMPLETED","reviews":[{"menuItemId":"food1","rating":5}],"items":[]}]}"""))
        assertEquals(setOf("food1"), repository.getHistory(1).orders.single().reviewedMenuItemIds)
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"Bạn đã đánh giá món này rồi"}"""))
        try { repository.submitReview("order1", "food1", 5, ""); fail() }
        catch (error: FoodHubApiException) { assertEquals(400, error.statusCode) }
    }
}
