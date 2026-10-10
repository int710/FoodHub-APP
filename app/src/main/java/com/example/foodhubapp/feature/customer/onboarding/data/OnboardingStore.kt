package com.example.foodhubapp.feature.customer.onboarding.data

import android.content.Context

class OnboardingStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    fun hasCompleted(): Boolean = preferences.getBoolean(KEY_COMPLETED, false)

    fun complete(): Boolean = preferences.edit().putBoolean(KEY_COMPLETED, true).commit()

    private companion object {
        const val KEY_COMPLETED = "completed"
    }
}
