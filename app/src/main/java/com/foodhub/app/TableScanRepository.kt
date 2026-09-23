package com.foodhub.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class TableSession(
    val tableId: String,
    val tableName: String,
    val floor: String?,
    val tableToken: String,
)

class TableScanException(message: String) : IOException(message)

class TableSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("table_session", Context.MODE_PRIVATE)

    fun save(session: TableSession) {
        val previousToken = preferences.getString(KEY_TABLE_TOKEN, null)
        preferences.edit()
            .putString(KEY_TABLE_ID, session.tableId)
            .putString(KEY_TABLE_NAME, session.tableName)
            .putString(KEY_FLOOR, session.floor)
            .putString(KEY_TABLE_TOKEN, session.tableToken)
            .apply {
                if (previousToken != session.tableToken) remove(KEY_CONVERSATION_ID)
            }
            .apply()
    }

    fun current(): TableSession? {
        val token = preferences.getString(KEY_TABLE_TOKEN, null) ?: return null
        return TableSession(
            tableId = preferences.getString(KEY_TABLE_ID, "").orEmpty(),
            tableName = preferences.getString(KEY_TABLE_NAME, "Bàn đã quét").orEmpty(),
            floor = preferences.getString(KEY_FLOOR, null),
            tableToken = token,
        )
    }

    fun clear() = preferences.edit().clear().apply()

    fun conversationId(): String? = preferences.getString(KEY_CONVERSATION_ID, null)

    fun saveConversationId(conversationId: String) {
        preferences.edit().putString(KEY_CONVERSATION_ID, conversationId).apply()
    }

    private companion object {
        const val KEY_TABLE_ID = "table_id"
        const val KEY_TABLE_NAME = "table_name"
        const val KEY_FLOOR = "floor"
        const val KEY_TABLE_TOKEN = "table_token"
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
    private val sessionStore = TableSessionStore(context.applicationContext)

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
                tableToken = tableToken,
            )
            sessionStore.save(tableSession)
            tableSession
        }
    }

    private fun extractQrToken(rawValue: String): String {
        val value = rawValue.trim()
        if (value.isBlank()) return ""

        value.asJsonObject()?.optString("qrToken")
            ?.takeIf(String::isNotBlank)
            ?.let { return it }

        runCatching { Uri.parse(value) }.getOrNull()?.let { uri ->
            listOf("qrToken", "qr_token", "token")
                .firstNotNullOfOrNull { key -> uri.getQueryParameter(key)?.takeIf(String::isNotBlank) }
                ?.let { return it }
        }

        return value
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
