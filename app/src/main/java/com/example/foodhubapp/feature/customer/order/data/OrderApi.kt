package com.example.foodhubapp.feature.customer.order.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface OrderApi {

    @POST("order/{type}/new")
    fun createOrder(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("type") type: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @GET("order/history")
    fun getHistory(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("type") type: String? = null
    ): Call<JsonObject>

    @PATCH("order/{id}/cancel")
    fun cancel(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @GET("order/table-history")
    fun getTableHistory(
        @HeaderMap headers: Map<String, String>,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("type") type: String? = null
    ): Call<JsonObject>
}
