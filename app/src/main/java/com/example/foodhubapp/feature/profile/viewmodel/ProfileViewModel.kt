package com.example.foodhubapp.feature.profile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.session.SessionManager
import com.example.foodhubapp.feature.profile.data.ProfileRepository
import com.example.foodhubapp.feature.profile.data.RemoteProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val tokenStore = TokenStore(application.applicationContext)
    private val sessionManager = SessionManager(
        tokenStore = tokenStore
    )
    private val profileRepository: ProfileRepository = RemoteProfileRepository(
        tokenStore = tokenStore
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            profileRepository.getProfileData()
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không tải được hồ sơ"
                        )
                    }
                }
        }
    }

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
