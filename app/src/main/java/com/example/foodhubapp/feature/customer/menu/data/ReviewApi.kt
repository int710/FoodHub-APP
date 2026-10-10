package com.example.foodhubapp.feature.customer.menu.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface ReviewApi {

    @GET("reviews/items/{id}")
    fun getReviews(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Call<JsonObject>

    @POST("reviews/feedback")
    fun submitReview(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>
}
