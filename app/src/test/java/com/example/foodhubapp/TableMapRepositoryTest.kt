package com.example.foodhubapp

import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.table.data.RemoteTableMapRepository
import com.example.foodhubapp.feature.table.data.RestaurantTableStatus
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TableMapRepositoryTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After fun tearDown() = server.shutdown()

    @Test fun parsesRestaurantTableMapStatuses() = runBlocking {
        server.enqueue(MockResponse().setBody("""{
            "data": [
                {"id":"t1","name":"Bàn 01","capacity":2,"floor":"Tầng 1","isActive":true,"status":"AVAILABLE"},
                {"id":"t2","name":"Bàn 02","capacity":4,"floor":"Tầng 1","isActive":true,"status":"OCCUPIED"},
                {"id":"t3","name":"Sân vườn","capacity":6,"floor":null,"isActive":false,"status":"INACTIVE"}
            ]
        }"""))
        val repository = RemoteTableMapRepository(FoodHubApiClient(server.url("/api/v1").toString()))

        val tables = repository.getTables()

        assertEquals("/api/v1/table", server.takeRequest().path)
        assertEquals(listOf(
            RestaurantTableStatus.AVAILABLE,
            RestaurantTableStatus.OCCUPIED,
            RestaurantTableStatus.INACTIVE
        ), tables.map { it.status })
        assertEquals(6, tables.last().capacity)
    }
}
