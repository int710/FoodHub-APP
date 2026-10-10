package com.example.foodhubapp.feature.admin.order.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface AdminOrderApi {

    @GET("order/history")
    fun getOrders(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Call<JsonObject>

    @GET("order/kitchen")
    fun getKitchenItems(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Call<JsonObject>

    @PATCH("order/{id}/confirm")
    fun confirm(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>

    @PATCH("order/{id}/reject")
    fun reject(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>

    @PATCH("order/{id}/serve")
    fun serve(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>

    @PATCH("order/{id}/complete")
    fun complete(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>

    @PATCH("order/kitchen/{id}/status")
    fun updateItem(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @PATCH("payment/{id}/cash-confirm")
    fun confirmCash(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>

    @PATCH("payment/{id}/zalopay/convert")
    fun convertCashToZaloPay(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>
}
