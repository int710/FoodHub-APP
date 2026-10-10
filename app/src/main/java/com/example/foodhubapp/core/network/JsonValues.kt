package com.example.foodhubapp.core.network

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

/** Shared Gson configuration. Explicit nulls clear optional fields in PATCH requests. */
val foodHubGson = GsonBuilder().serializeNulls().create()

fun parseJsonObject(text: String): JsonObject {
    val value = JsonParser.parseString(text)
    if (!value.isJsonObject) throw JsonParseException("Expected a JSON object")
    return value.asJsonObject
}

fun jsonArray(values: Iterable<*>): JsonArray = JsonArray().apply {
    values.forEach { add(it.toJsonElement()) }
}

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull.INSTANCE
    is JsonElement -> this
    else -> foodHubGson.toJsonTree(this)
}

/** Preserve the API's mixed number/string values and optional response fields. */
private fun JsonElement?.value(): Any? = when {
    this == null || isJsonNull -> null
    isJsonObject -> asJsonObject
    isJsonArray -> asJsonArray
    asJsonPrimitive.isBoolean -> asBoolean
    asJsonPrimitive.isNumber -> asNumber
    else -> asString
}

fun JsonObject.put(key: String, value: Any?): JsonObject = apply { add(key, value.toJsonElement()) }
fun JsonArray.put(value: Any?): JsonArray = apply { add(value.toJsonElement()) }
fun JsonObject.opt(key: String): Any? = get(key).value()
fun JsonArray.opt(index: Int): Any? = if (index in 0 until size()) get(index).value() else null
fun JsonObject.isNull(key: String): Boolean = get(key)?.isJsonNull != false
fun JsonObject.optObject(key: String): JsonObject? = opt(key) as? JsonObject
fun JsonObject.optArray(key: String): JsonArray? = opt(key) as? JsonArray
fun JsonArray.optObject(index: Int): JsonObject? = opt(index) as? JsonObject
fun JsonObject.getObject(key: String): JsonObject =
    optObject(key) ?: throw JsonParseException("Missing object: $key")

fun JsonObject.getArray(key: String): JsonArray =
    optArray(key) ?: throw JsonParseException("Missing array: $key")

fun JsonArray.getObject(index: Int): JsonObject =
    optObject(index) ?: throw JsonParseException("Missing object at index $index")

fun JsonObject.optString(key: String, fallback: String = ""): String =
    opt(key)?.toString() ?: fallback

fun JsonObject.getString(key: String): String =
    opt(key)?.toString() ?: throw JsonParseException("Missing string: $key")

fun JsonArray.getString(index: Int): String =
    opt(index)?.toString() ?: throw JsonParseException("Missing string at index $index")

fun JsonObject.optInt(key: String, fallback: Int = 0): Int = when (val value = opt(key)) {
    is Number -> value.toInt()
    is String -> value.toDoubleOrNull()?.toInt() ?: fallback
    else -> fallback
}

fun JsonObject.optLong(key: String, fallback: Long = 0): Long = when (val value = opt(key)) {
    is Number -> value.toLong()
    is String -> value.toDoubleOrNull()?.toLong() ?: fallback
    else -> fallback
}

fun JsonObject.optDouble(key: String, fallback: Double = Double.NaN): Double =
    opt(key)?.toString()?.toDoubleOrNull() ?: fallback

fun JsonObject.optBoolean(key: String, fallback: Boolean = false): Boolean =
    opt(key)?.toString()?.lowercase()?.toBooleanStrictOrNull() ?: fallback

fun JsonObject.getInt(key: String): Int = getString(key).toDouble().toInt()
fun JsonObject.getLong(key: String): Long = when (val value = opt(key)) {
    is Number -> value.toLong()
    is String -> value.toDoubleOrNull()?.toLong() ?: throw JsonParseException("Invalid long: $key")
    else -> throw JsonParseException("Missing long: $key")
}

fun JsonObject.getBoolean(key: String): Boolean = getString(key).lowercase().toBooleanStrictOrNull()
    ?: throw JsonParseException("Invalid boolean: $key")
