package com.example.foodhubapp.feature.table.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface TableApi {

    @GET("table")
    fun getTables(
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Call<JsonObject>

    @POST("table/scan")
    fun scan(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @POST("table/session/end")
    fun endSession(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>
}
