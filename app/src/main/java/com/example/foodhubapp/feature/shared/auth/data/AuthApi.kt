package com.example.foodhubapp.feature.shared.auth.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface AuthApi {

    @POST("user/login")
    fun login(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @POST("user/register")
    fun register(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @POST("user/forgot-password")
    fun forgotPassword(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @POST("user/reset-password")
    fun resetPassword(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @POST("user/verify-email")
    fun verifyEmail(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @POST("user/logout")
    fun logout(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @POST("user/refresh-token")
    fun refreshToken(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>

    @GET("user/me")
    fun getProfile(
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Call<JsonObject>

    @PATCH("user/me")
    fun updateProfile(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject
    ): Call<JsonObject>
}
