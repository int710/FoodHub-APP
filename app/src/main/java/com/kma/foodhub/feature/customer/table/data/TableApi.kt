package com.kma.foodhub.feature.customer.table.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST

/** API phục vụ riêng luồng khách quét QR và kết thúc phiên bàn. */
interface TableApi {
    @GET("table")
    fun getTables(
        @HeaderMap headers: Map<String, String> = emptyMap(),
    ): Call<JsonObject>

    @POST("table/scan")
    fun scan(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject,
    ): Call<JsonObject>

    @POST("table/session/end")
    fun endSession(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject = JsonObject(),
    ): Call<JsonObject>
}
