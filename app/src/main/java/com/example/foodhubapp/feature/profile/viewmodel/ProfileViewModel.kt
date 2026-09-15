package com.example.foodhubapp.feature.profile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(
        tokenStore = TokenStore(application.applicationContext)
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun toggleKitchenNotifications() {
        _uiState.update {
            it.copy(kitchenNotificationsEnabled = !it.kitchenNotificationsEnabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}
