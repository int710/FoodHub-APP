package com.example.foodhubapp.core.network

import android.net.Uri

fun resolveMediaUrl(url: String?): String? {
    val value = url?.trim()?.takeIf(String::isNotBlank) ?: return null
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return value
    if (!uri.host.orEmpty().endsWith("r2.cloudflarestorage.com", ignoreCase = true)) return value

    val fileName = uri.pathSegments.lastOrNull()?.takeIf(String::isNotBlank) ?: return value
    return "${FoodhubRetrofit.BASE_URL}media/file/${Uri.encode(fileName)}"
}
