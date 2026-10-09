package com.example.foodhubapp.core.network

import com.example.foodhubapp.feature.admin.chat.data.ConversationApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

object FoodhubRetrofit {
    const val BASE_URL = "https://foodhub-8lv1.onrender.com/api/v1/"
    val httpClient: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).build()
    private val retrofit = Retrofit.Builder().baseUrl(BASE_URL).client(httpClient)
        .addConverterFactory(GsonConverterFactory.create()).build()
    val conversationApi: ConversationApi = retrofit.create(ConversationApi::class.java)
}