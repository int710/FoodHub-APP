package com.example.foodhubapp

import com.example.foodhubapp.core.network.*
import com.example.foodhubapp.core.network.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SessionRefreshTest {
    private lateinit var server: MockWebServer
    private val client = OkHttpClient()
    private class Storage : SessionTokenStorage {
        var tokens = SessionTokens("old-access", "old-refresh")
        var beforeReplace: (() -> Unit)? = null
        override suspend fun read() = tokens
        override suspend fun replace(expected: SessionTokens, replacement: SessionTokens): Boolean {
            beforeReplace?.invoke()
            if (tokens != expected) return false
            tokens = replacement
            return true
        }
    }
    @Before fun setup() { server = MockWebServer(); server.start() }
    @After fun teardown() { server.shutdown() }
    private fun url() = server.url("/api/v1").toString().trimEnd('/')

    @Test fun simultaneousUnauthorizedRequestsShareOneTokenRefresh() {
        val store = Storage()
        val refresh = SessionRefreshCoordinator(store)
        server.enqueue(MockResponse().setBody("""{"data":{"access_token":"new-access","refresh_token":"new-refresh"}}"""))
        val executor = Executors.newFixedThreadPool(2)
        try {
            val requests = (1..2).map { executor.submit<String> { refresh.refresh(client, url(), "old-access") } }
            requests.forEach { assertEquals("new-access", it.get(5, TimeUnit.SECONDS)) }
            assertEquals(1, server.requestCount)
            assertEquals(SessionTokens("new-access", "new-refresh"), store.tokens)
        } finally { executor.shutdownNow() }
    }

    @Test fun temporaryServerErrorsAndMalformedResponsesPreserveSession() {
        for (response in listOf(
            MockResponse().setResponseCode(503).setBody("""{"message":"Temporarily unavailable"}"""),
            MockResponse().setResponseCode(502).setBody("<html>Bad gateway</html>"),
            MockResponse().setBody("""{"data":{}}""")
        )) {
            val store = Storage()
            server.enqueue(response)
            try { SessionRefreshCoordinator(store).refresh(client, url(), "old-access"); fail("Expected temporary failure") }
            catch (_: IllegalStateException) { }
            assertEquals(SessionTokens("old-access", "old-refresh"), store.tokens)
        }
    }

    @Test fun revokedRefreshTokenClearsOnlyTheRejectedSession() {
        val store = Storage()
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Token expired"}"""))
        assertNull(SessionRefreshCoordinator(store).refresh(client, url(), "old-access"))
        assertEquals(SessionTokens(null, null), store.tokens)
    }

    @Test fun successfulRefreshDoesNotUndoLogoutOrOverwriteAnotherLogin() {
        for (replacement in listOf(SessionTokens(null, null), SessionTokens("other-account", "other-refresh"))) {
            val store = Storage()
            store.beforeReplace = { store.tokens = replacement }
            server.enqueue(MockResponse().setBody("""{"data":{"access_token":"new-access","refresh_token":"new-refresh"}}"""))
            assertEquals(replacement.access, SessionRefreshCoordinator(store).refresh(client, url(), "old-access"))
            assertEquals(replacement, store.tokens)
        }
    }

    @Test fun rejectionOfOldRefreshTokenDoesNotLogOutANewerLogin() {
        val store = Storage()
        store.beforeReplace = { store.tokens = SessionTokens("other-account", "other-refresh") }
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Revoked"}"""))
        assertEquals("other-account", SessionRefreshCoordinator(store).refresh(client, url(), "old-access"))
        assertEquals(SessionTokens("other-account", "other-refresh"), store.tokens)
    }

    @Test fun profileRequestsRefreshAndRetryWithNewAuthorization() {
        val store = Storage()
        val coordinator = SessionRefreshCoordinator(store)
        val api = FoodHubApiClient(url(), client, coordinator::refresh)
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Expired access token"}"""))
        server.enqueue(MockResponse().setBody("""{"data":{"access_token":"new-access","refresh_token":"new-refresh"}}"""))
        server.enqueue(MockResponse().setBody("""{"data":{"id":"customer-1"}}"""))
        val profile = api.execute(api.authApi.getProfile(mapOf("Authorization" to "Bearer old-access")))
        assertEquals("customer-1", profile.getObject("data").getString("id"))
        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("/api/v1/user/refresh-token", server.takeRequest().path)
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
    }

    @Test fun refreshFailureOnOrderRequestIsNotReportedAsLoggedOut() {
        val store = Storage()
        val coordinator = SessionRefreshCoordinator(store)
        val api = FoodHubApiClient(url(), client, coordinator::refresh)
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Expired"}"""))
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"Retry later"}"""))
        try {
            api.execute(api.orderApi.getHistory(mapOf("Authorization" to "Bearer old-access"), 1, 20))
            fail("Expected server error")
        }
        catch (error: FoodHubApiException) { assertEquals(503, error.statusCode) }
        assertEquals(SessionTokens("old-access", "old-refresh"), store.tokens)
    }
}
