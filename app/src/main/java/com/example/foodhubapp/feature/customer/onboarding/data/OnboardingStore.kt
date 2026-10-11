package com.example.foodhubapp.feature.customer.onboarding.data

import android.content.Context
import androidx.core.content.edit

class OnboardingStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    fun hasCompleted(): Boolean = preferences.getBoolean(KEY_COMPLETED, false)

    fun complete() = preferences.edit(commit = true) { putBoolean(KEY_COMPLETED, true) }

    private companion object {
        const val KEY_COMPLETED = "completed"
    }
}
