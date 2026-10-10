package com.example.foodhubapp.feature.shared.payment

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface PaymentApi {

    @GET("payment/{id}")
    fun getDetail(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String
    ): Call<JsonObject>

    @POST("payment/{provider}/create")
    fun createPayment(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("provider") provider: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @GET("payment/{provider}/status/{code}")
    fun getStatus(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("provider") provider: String,
        @Path("code", encoded = true) code: String
    ): Call<JsonObject>
}
