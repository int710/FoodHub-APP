package com.example.foodhubapp.feature.admin.menu.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface AdminMenuApi {

    @GET("menu/items")
    fun getItems(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Call<JsonObject>

    @GET("menu/categories")
    fun getCategories(
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Call<JsonObject>

    @GET("menu/item/{id}")
    fun getItem(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String
    ): Call<JsonObject>

    @POST("menu/categories")
    fun createCategory(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @PUT("menu/categories/{id}")
    fun updateCategory(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @DELETE("menu/categories/{id}")
    fun deleteCategory(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String
    ): Call<JsonObject>

    @POST("menu/items")
    fun createItem(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @PATCH("menu/items/{id}")
    fun updateItem(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @PATCH("menu/items/{id}/toggle")
    fun toggleItem(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>

    @DELETE("menu/items/{id}")
    fun deleteItem(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String
    ): Call<JsonObject>

    @POST("menu/item/flash-sales")
    fun createFlashSale(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @DELETE("menu/item/flash-sales/{id}")
    fun deleteFlashSale(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String
    ): Call<JsonObject>

    @POST("menu/items/{id}/variants")
    fun createVariant(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @PATCH("menu/item-variants/{id}")
    fun updateVariant(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject
    ): Call<JsonObject>
}
