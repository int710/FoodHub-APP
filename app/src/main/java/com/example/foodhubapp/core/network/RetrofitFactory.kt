package com.example.foodhubapp.core.network

import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

/** Handles empty 2xx bodies while keeping malformed/HTML responses visible to the UI. */
private object JsonObjectConverter : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type, annotations: Array<out Annotation>, retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type != JsonObject::class.java) return null
        return Converter<ResponseBody, JsonObject> { body ->
            body.use {
                val text = it.string()
                if (text.isBlank()) JsonObject() else {
                    if (!text.trimStart().startsWith("{")) {
                        throw IllegalStateException("Máy chủ chưa trả dữ liệu JSON. Vui lòng thử lại sau.")
                    }
                    parseJsonObject(text)
                }
            }
        }
    }
}

internal fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl.trimEnd('/') + "/")
    .client(client)
    .addConverterFactory(JsonObjectConverter)
    .addConverterFactory(GsonConverterFactory.create(foodHubGson))
    .build()
