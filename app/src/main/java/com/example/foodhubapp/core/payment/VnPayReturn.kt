package com.example.foodhubapp.core.payment

import android.net.Uri

data class VnPayReturn(
    val orderCode: String,
    val result: String?,
    val responseCode: String?
)

fun Uri.toVnPayReturnOrNull(): VnPayReturn? {
    if (scheme != "foodhub" || host != "payment" || path != "/result") return null
    val orderCode = getQueryParameter("orderCode")?.takeIf { it.isNotBlank() } ?: return null
    return VnPayReturn(
        orderCode = orderCode,
        result = getQueryParameter("result"),
        responseCode = getQueryParameter("responseCode")
    )
}
