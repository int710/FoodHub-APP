package com.example.foodhubapp.feature.customer.cart.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface CartApi {

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
