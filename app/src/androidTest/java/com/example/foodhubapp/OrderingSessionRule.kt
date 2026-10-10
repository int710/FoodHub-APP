package com.example.foodhubapp

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/** Tests start without a table session or saved delivery mode, then restore the user's state. */
class OrderingSessionRule : ExternalResource() {
    private val snapshots = mutableListOf<Pair<SharedPreferences, Map<String, *>>>()

    override fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        for (name in listOf("ordering_context", "table_session")) {
            val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            snapshots += preferences to preferences.all.toMap()
            check(preferences.edit().clear().commit())
        }
    }

    override fun after() {
        for ((preferences, values) in snapshots) {
            val editor = preferences.edit().clear()
            for ((key, value) in values) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            check(editor.commit())
        }
        snapshots.clear()
    }
}
