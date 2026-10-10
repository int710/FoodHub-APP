package com.example.foodhubapp.feature.customer.menu.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface MenuApi {

    @GET("menu/categories")
    fun getCategories(): Call<JsonObject>

    @GET("menu/all")
    fun getMenu(
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Call<JsonObject>

    @GET("menu/item/{id}")
    fun getFood(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String
    ): Call<JsonObject>
}
