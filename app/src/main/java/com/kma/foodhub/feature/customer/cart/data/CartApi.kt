package com.kma.foodhub.feature.customer.cart.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface CartApi {

    @GET("vouchers/my")
    fun getMyVouchers(
        @HeaderMap headers: Map<String, String>,
        @Query("orderType") orderType: String,
    ): Call<JsonObject>

    @POST("cart/{type}/voucher")
    fun applyVoucher(
        @Path("type") type: String,
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject,
    ): Call<JsonObject>

    @DELETE("cart/{type}/voucher")
    fun removeVoucher(
        @Path("type") type: String,
        @HeaderMap headers: Map<String, String> = emptyMap(),
    ): Call<JsonObject>

    @GET("cart/{type}/items")
    fun getItems(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("type") type: String
    ): Call<JsonObject>

    @POST("cart/{type}/items/add")
    fun addItem(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("type") type: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @PATCH("cart/{type}/items/{id}")
    fun updateItem(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("type") type: String,
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @DELETE("cart/{type}/items/{id}")
    fun deleteItem(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("type") type: String,
        @Path("id", encoded = true) id: String
    ): Call<JsonObject>

    @DELETE("cart/{type}/clear")
    fun clear(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("type") type: String
    ): Call<JsonObject>
}
