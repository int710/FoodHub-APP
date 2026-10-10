package com.example.foodhubapp.core.network

import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Retry an authenticated request once; never refresh authentication endpoints themselves. */
class TokenAuthenticator(
    private val baseUrl: String,
    private val refresh: (String?) -> String?
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (generateSequence(response.priorResponse) { it.priorResponse }.any { it.code == 401 }) return null
        val request = response.request
        val authorization = request.header("Authorization") ?: return null
        val path = request.url.encodedPath.removePrefix(
            (baseUrl.trimEnd('/') + "/").toHttpUrl().encodedPath
        )
        if (path in AUTH_ENDPOINTS) return null
        val token = refresh(authorization.removePrefix("Bearer "))
            ?.takeIf(String::isNotBlank) ?: return null
        return request.newBuilder().header("Authorization", "Bearer $token").build()
    }

    private companion object {
        val AUTH_ENDPOINTS = setOf(
            "user/login", "user/register", "user/refresh-token", "user/logout",
            "user/forgot-password", "user/reset-password", "user/verify-email"
        )
    }
}
