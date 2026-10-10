package com.example.foodhubapp.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** One production configuration; tests can inject their own URL and HTTP client. */
object FoodhubRetrofit {
    const val BASE_URL = "https://foodhub-8lv1.onrender.com/api/v1/"

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiClient: FoodHubApiClient by lazy { FoodHubApiClient(BASE_URL, httpClient) }

}
