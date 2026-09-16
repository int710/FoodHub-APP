package com.example.foodhubapp.feature.profile.viewmodel

import com.example.foodhubapp.feature.profile.model.ProfileUiModel
import com.example.foodhubapp.feature.profile.model.previewProfile

data class ProfileUiState(
    val profile: ProfileUiModel = previewProfile,
    val kitchenNotificationsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)
