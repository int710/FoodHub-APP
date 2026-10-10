package com.example.foodhubapp.core.network

import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import okhttp3.OkHttpClient

object FoodHubSessionRefresh {
    @Volatile
    private var coordinator: SessionRefreshCoordinator? = null

    @Synchronized
    fun initialize(context: Context) {
        if (coordinator != null) return
        val store = TokenStore(context.applicationContext)
        coordinator = SessionRefreshCoordinator(object : SessionTokenStorage {
            override suspend fun read() = store.sessionTokens()
            override suspend fun replace(expected: SessionTokens, replacement: SessionTokens) =
                store.replaceSessionTokens(expected, replacement)
        })
    }

    fun refresh(
        httpClient: OkHttpClient,
        baseUrl: String,
        rejectedAccessToken: String? = null
    ): String? = coordinator?.refresh(httpClient, baseUrl, rejectedAccessToken)
}
