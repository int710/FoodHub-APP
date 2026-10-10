package com.example.foodhubapp.core.network

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*
import okhttp3.MultipartBody

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface MediaApi {

    @Multipart
    @POST("media/upload-image")
    fun uploadImage(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Part image: MultipartBody.Part
    ): Call<JsonObject>
}
