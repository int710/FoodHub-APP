package com.example.foodhubapp.core.network

import com.example.foodhubapp.feature.admin.menu.data.AdminMenuApi
import com.example.foodhubapp.feature.admin.order.data.AdminOrderApi
import com.example.foodhubapp.feature.admin.table.data.AdminTableApi
import com.example.foodhubapp.feature.shared.auth.data.AuthApi
import com.example.foodhubapp.feature.customer.menu.data.MenuApi
import com.example.foodhubapp.feature.customer.cart.data.CartApi
import com.example.foodhubapp.feature.customer.order.data.OrderApi
import com.example.foodhubapp.feature.table.data.TableApi
import com.example.foodhubapp.feature.shared.notification.data.NotificationApi
import com.example.foodhubapp.feature.shared.payment.PaymentApi
import com.example.foodhubapp.feature.customer.menu.data.ReviewApi
import com.example.foodhubapp.feature.admin.chat.data.ConversationApi
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response

/** Shared Retrofit transport, Gson serialization, session retry and backend error messages. */
class FoodHubApiClient(
    baseUrl: String = FoodhubRetrofit.BASE_URL,
    httpClient: OkHttpClient = FoodhubRetrofit.httpClient,
    refreshSession: (OkHttpClient, String, String?) -> String? = FoodHubSessionRefresh::refresh,
) {
    private val client = httpClient.newBuilder()
        .addInterceptor(AuthInterceptor())
        .authenticator(TokenAuthenticator(baseUrl) { rejectedToken ->
            // Refresh uses the original client, without this authenticator, to avoid recursion.
            refreshSession(httpClient, baseUrl.trimEnd('/'), rejectedToken)
        })
        .build()
    private val retrofit = createRetrofit(baseUrl, client)

    val adminMenuApi: AdminMenuApi by lazy { retrofit.create(AdminMenuApi::class.java) }
    val adminOrderApi: AdminOrderApi by lazy { retrofit.create(AdminOrderApi::class.java) }
    val adminTableApi: AdminTableApi by lazy { retrofit.create(AdminTableApi::class.java) }
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val menuApi: MenuApi by lazy { retrofit.create(MenuApi::class.java) }
    val cartApi: CartApi by lazy { retrofit.create(CartApi::class.java) }
    val orderApi: OrderApi by lazy { retrofit.create(OrderApi::class.java) }
    val tableApi: TableApi by lazy { retrofit.create(TableApi::class.java) }
    val notificationApi: NotificationApi by lazy { retrofit.create(NotificationApi::class.java) }
    val paymentApi: PaymentApi by lazy { retrofit.create(PaymentApi::class.java) }
    val reviewApi: ReviewApi by lazy { retrofit.create(ReviewApi::class.java) }
    val conversationApi: ConversationApi by lazy { retrofit.create(ConversationApi::class.java) }
    val mediaApi: MediaApi by lazy { retrofit.create(MediaApi::class.java) }

    fun execute(call: Call<JsonObject>): JsonObject = call.execute().jsonOrThrow()

    fun uploadImage(
        path: String, fileName: String, mimeType: String, bytes: ByteArray,
        headers: Map<String, String> = emptyMap(),
    ): JsonObject {
        require(path == "/media/upload-image") { "Unsupported image endpoint" }
        val image = MultipartBody.Part.createFormData(
            "image", fileName, bytes.toRequestBody(mimeType.toMediaType())
        )
        return execute(mediaApi.uploadImage(headers, image))
    }

}

class FoodHubApiException(val statusCode: Int, message: String) : IllegalStateException(message)

internal fun Response<JsonObject>.jsonOrThrow(): JsonObject {
    if (!isSuccessful) {
        val text = errorBody()?.use { it.string() }.orEmpty()
        throw FoodHubApiException(code(), parseApiError(code(), text))
    }
    return body() ?: JsonObject()
}

internal fun parseApiError(code: Int, body: String): String {
    if (body.trimStart().startsWith("<")) {
        return "Máy chủ đang chặn yêu cầu hoặc chưa sẵn sàng (HTTP $code). Vui lòng thử lại sau."
    }
    return runCatching {
        val root = parseJsonObject(body)
        val errors = root.optObject("errors")
        if (errors != null && errors.size() > 0) {
            errors.keySet().mapNotNull { key -> errors.optString(key).takeIf(String::isNotBlank) }
                .joinToString("\n")
        } else root.optString("message").ifBlank { "API lỗi $code" }
    }.getOrDefault("API lỗi $code")
}
