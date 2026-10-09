package com.example.foodhubapp.feature.customer.table.data

import android.content.Context
import com.example.foodhubapp.feature.customer.cart.data.CartType
import com.example.foodhubapp.feature.customer.order.data.OrderingContextStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

data class TableSession(
    val tableId: String,
    val tableName: String,
    val floor: String?,
    val capacity: Int? = null,
    val tableToken: String,
    val sessionId: String? = null,
    val expiresAtEpochMillis: Long? = null,
)

class TableScanException(message: String) : IOException(message)

class TableSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("table_session", Context.MODE_PRIVATE)

    fun save(session: TableSession) {
        val previousToken = preferences.getString(KEY_TABLE_TOKEN, null)
        val editor = preferences.edit()
            .putString(KEY_TABLE_ID, session.tableId)
            .putString(KEY_TABLE_NAME, session.tableName)
            .putString(KEY_FLOOR, session.floor)
            .putInt(KEY_CAPACITY, session.capacity ?: 0)
            .putString(KEY_TABLE_TOKEN, session.tableToken)
            .putString(KEY_SESSION_ID, session.sessionId)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochMillis ?: 0L)
        if (previousToken != session.tableToken) editor.remove(KEY_CONVERSATION_ID)
        editor.commit()
    }

    fun current(): TableSession? {
        val token = preferences.getString(KEY_TABLE_TOKEN, null) ?: return null
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L).takeIf { it > 0L }
        if (expiresAt != null && System.currentTimeMillis() >= expiresAt) {
            clear()
            return null
        }
        return TableSession(
            tableId = preferences.getString(KEY_TABLE_ID, "").orEmpty(),
            tableName = preferences.getString(KEY_TABLE_NAME, "Bàn đã quét").orEmpty(),
            floor = preferences.getString(KEY_FLOOR, null),
            capacity = preferences.getInt(KEY_CAPACITY, 0).takeIf { it > 0 },
            tableToken = token,
            sessionId = preferences.getString(KEY_SESSION_ID, null),
            expiresAtEpochMillis = expiresAt,
        )
    }

    fun clear() = preferences.edit().clear().commit()

    fun conversationId(): String? = preferences.getString(KEY_CONVERSATION_ID, null)

    fun clearConversationId() = preferences.edit().remove(KEY_CONVERSATION_ID).commit()

    fun saveConversationId(conversationId: String) {
        preferences.edit().putString(KEY_CONVERSATION_ID, conversationId).apply()
    }

    private companion object {
        const val KEY_TABLE_ID = "table_id"
        const val KEY_TABLE_NAME = "table_name"
        const val KEY_FLOOR = "floor"
        const val KEY_CAPACITY = "capacity"
        const val KEY_TABLE_TOKEN = "table_token"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_CONVERSATION_ID = "conversation_id"
    }
}

class TableScanRepository(
    context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    private val appContext = context.applicationContext
    private val sessionStore = TableSessionStore(appContext)

    suspend fun scan(rawQrValue: String): TableSession = withContext(Dispatchers.IO) {
        val qrToken = extractQrToken(rawQrValue)
        if (qrToken.isBlank()) throw TableScanException("Mã QR không chứa token hợp lệ")

        val body = JSONObject()
            .put("qrToken", qrToken)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("https://foodhub-8lv1.onrender.com/api/v1/table/scan")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            val root = responseText.asJsonObject()
            val message = root?.optString("message")
                ?.takeIf(String::isNotBlank)
                ?: "Không thể kết nối với bàn"

            if (!response.isSuccessful) {
                throw TableScanException(message)
            }

            val data = root?.optJSONObject("data")
                ?: throw TableScanException(message)
            val table = data.optJSONObject("table") ?: data
            val session = data.optJSONObject("session")
            val tableToken = data.firstString("tableToken", "table_token", "token")
                ?: session?.firstString("tableToken", "table_token", "token")
                ?: throw TableScanException(message)

            val tableSession = TableSession(
                tableId = table.firstString("id", "tableId").orEmpty(),
                tableName = table.firstString("name", "tableName") ?: "Bàn đã quét",
                floor = table.firstString("floor"),
                capacity = table.optInt("capacity", 0).takeIf { it > 0 },
                tableToken = tableToken,
                sessionId = data.firstString("sessionId", "session_id"),
                expiresAtEpochMillis = data.optLong("expiresIn", 0L)
                    .takeIf { it > 0L }
                    ?.let { System.currentTimeMillis() + it * 1_000L },
            )
            sessionStore.save(tableSession)
            OrderingContextStore(appContext).select(CartType.DINE_IN)
            tableSession
        }
    }

    suspend fun endSession() = withContext(Dispatchers.IO) {
        val session = sessionStore.current() ?: return@withContext
        val request = Request.Builder()
            .url("https://foodhub-8lv1.onrender.com/api/v1/table/session/end")
            .header("X-Table-Token", session.tableToken)
            .post(JSONObject().toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val message = response.body?.string().orEmpty().asJsonObject()?.optString("message")
                throw TableScanException(message?.takeIf(String::isNotBlank) ?: "Không thể kết thúc phiên bàn")
            }
        }
        sessionStore.clear()
    }

    private fun String.asJsonObject(): JSONObject? = runCatching { JSONObject(this) }.getOrNull()

    private fun JSONObject.firstString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
        }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/** Chấp nhận đúng ba định dạng QR mà backend/admin đang phát: JSON, URL và token thô. */
internal fun extractQrToken(rawValue: String): String {
    val value = rawValue.trim()
    if (value.isBlank()) return ""

    runCatching { JSONObject(value) }.getOrNull()
        ?.optString("qrToken")
        ?.takeIf(String::isNotBlank)
        ?.let { return it }

    val query = runCatching { URI(value).rawQuery }.getOrNull()
    query?.split('&')
        ?.mapNotNull { parameter ->
            val parts = parameter.split('=', limit = 2)
            if (parts.size != 2) null
            else URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
        }
        ?.firstOrNull { (key, token) -> key in setOf("qrToken", "qr_token", "token") && token.isNotBlank() }
        ?.second
        ?.let { return it }

    return value
}
