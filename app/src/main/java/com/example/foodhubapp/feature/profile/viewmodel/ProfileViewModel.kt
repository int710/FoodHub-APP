package com.example.foodhubapp.feature.profile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.session.SessionManager
import com.example.foodhubapp.feature.auth.model.UserDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: UserDto? = null,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = TokenStore(application.applicationContext)
    private val sessionManager = SessionManager(tokenStore)
    private val state = MutableStateFlow(ProfileUiState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            if (!sessionManager.isLoggedIn()) {
                state.update { it.copy(isLoggedOut = true) }
            } else {
                tokenStore.user.collect { user -> state.update { it.copy(user = user) } }
            }
        }
    }

    fun logout() {
        if (state.value.isLoggingOut || state.value.isLoggedOut) return
        state.update { it.copy(isLoggingOut = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                sessionManager.logout()
                state.update { ProfileUiState(isLoggedOut = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(isLoggingOut = false, errorMessage = "Không thể đăng xuất. Vui lòng thử lại.") }
            }
        }
    }
}
