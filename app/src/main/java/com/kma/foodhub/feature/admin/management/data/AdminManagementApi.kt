package com.kma.foodhub.feature.admin.management.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface AdminManagementApi {
    @GET("vouchers/admin")
    fun getVouchers(@HeaderMap headers: Map<String, String>): Call<JsonObject>

    @POST("vouchers/admin")
    fun createVoucher(@HeaderMap headers: Map<String, String>, @Body body: JsonObject): Call<JsonObject>

    @PATCH("vouchers/admin/{id}")
    fun updateVoucher(@HeaderMap headers: Map<String, String>, @Path("id") id: String, @Body body: JsonObject): Call<JsonObject>

    @DELETE("vouchers/admin/{id}")
    fun deleteVoucher(@HeaderMap headers: Map<String, String>, @Path("id") id: String): Call<JsonObject>

    @GET("reviews/admin")
    fun getReviews(
        @HeaderMap headers: Map<String, String>,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("rating") rating: Int? = null,
        @Query("search") search: String? = null,
    ): Call<JsonObject>

    @DELETE("reviews/admin/{id}")
    fun deleteReview(
        @HeaderMap headers: Map<String, String>,
        @Path("id", encoded = true) id: String,
    ): Call<JsonObject>

    @GET("settings")
    fun getSettings(@HeaderMap headers: Map<String, String>): Call<JsonObject>

    @PATCH("settings")
    fun updateSettings(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject,
    ): Call<JsonObject>
}
