package com.example.foodhubapp.feature.admin.table.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface AdminTableApi {

    @GET("table")
    fun getTables(
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Call<JsonObject>

    @GET("table/{id}")
    fun getTable(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String
    ): Call<JsonObject>

    @POST("table/new")
    fun createTable(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @GET("table/{id}/qr")
    fun getQrContent(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String
    ): Call<JsonObject>

    @PATCH("table/{id}/toggle")
    fun toggleTable(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>

    @POST("table/{id}/regenerate-qr")
    fun regenerateQrContent(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>

}
