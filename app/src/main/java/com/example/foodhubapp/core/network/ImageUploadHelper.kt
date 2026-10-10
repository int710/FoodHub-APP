package com.example.foodhubapp.core.network

import android.content.Context
import android.net.Uri
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun uploadImageFromDevice(
    context: Context,
    uri: Uri,
    accessToken: String,
    filePrefix: String,
    apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient,
): String = withContext(Dispatchers.IO) {
    val resolver = context.applicationContext.contentResolver
    val mimeType = resolver.getType(uri)?.takeIf { it.startsWith("image/") }
        ?: throw IllegalArgumentException("Tệp đã chọn không phải là ảnh")
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalStateException("Không đọc được ảnh đã chọn")
    require(bytes.isNotEmpty()) { "Ảnh đã chọn không có dữ liệu" }
    require(bytes.size <= 5 * 1024 * 1024) { "Ảnh phải nhỏ hơn hoặc bằng 5 MB" }

    val extension = when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> mimeType.substringAfter('/').substringBefore('+').ifBlank { "jpg" }
    }
    val response = apiClient.uploadImage(
        path = "/media/upload-image",
        fileName = "$filePrefix-${System.currentTimeMillis()}.$extension",
        mimeType = mimeType,
        bytes = bytes,
        headers = mapOf("Authorization" to "Bearer $accessToken"),
    )
    val data = response.opt("data") as? JsonArray
    data?.optObject(0)?.optString("url")?.takeIf(String::isNotBlank)
        ?: throw IllegalStateException("Máy chủ không trả về đường dẫn ảnh")
}
