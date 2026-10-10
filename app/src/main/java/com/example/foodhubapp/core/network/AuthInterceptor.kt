package com.example.foodhubapp.core.network

import okhttp3.Interceptor
import okhttp3.Response

/** Keep authentication explicit: public and table-only endpoints must not inherit a login. */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().newBuilder().header("Accept", "application/json").build()
    )
}
