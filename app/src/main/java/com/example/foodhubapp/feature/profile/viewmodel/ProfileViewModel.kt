package com.example.foodhubapp.feature.profile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.core.session.SessionManager
import com.example.foodhubapp.feature.auth.model.UserDto
import com.example.foodhubapp.feature.profile.data.ProfileRepository
import com.example.foodhubapp.feature.profile.data.RemoteProfileRepository
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

class ProfileViewModel @JvmOverloads constructor(
    application: Application,
    private val profileRepository: ProfileRepository = RemoteProfileRepository(
        tokenStore = TokenStore(application.applicationContext)
    )
) : AndroidViewModel(application) {
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
        refreshProfile()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            if (!sessionManager.isLoggedIn()) return@launch
            val cachedUser = tokenStore.getUser()
            profileRepository.getProfileData()
                .onSuccess { profile ->
                    val updatedUser = (state.value.user ?: cachedUser)?.copy(
                        fullName = profile.user.name,
                        phoneNumber = profile.user.phone,
                        email = profile.user.email,
                        role = profile.user.role,
                    )
                    updatedUser?.let { tokenStore.saveUser(it) }
                    state.update { current ->
                        current.copy(
                            user = updatedUser,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    if (error is FoodHubApiException && error.statusCode == 401) {
                        sessionManager.logout()
                        state.update { it.copy(isLoggedOut = true) }
                    } else state.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    fun logout() {
        if (state.value.isLoggingOut || state.value.isLoggedOut) return
        state.update { it.copy(isLoggingOut = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                profileRepository.logout().getOrThrow()
                state.update { ProfileUiState(isLoggedOut = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(isLoggingOut = false, errorMessage = "Không thể đăng xuất. Vui lòng thử lại.") }
            }
        }
    }

    fun updateProfile(name: String, phone: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val cachedUser = tokenStore.getUser()
            profileRepository.updateProfile(name, phone)
                .onSuccess { profile ->
                    val updatedUser = (state.value.user ?: cachedUser)?.copy(
                        fullName = profile.user.name,
                        phoneNumber = profile.user.phone,
                        email = profile.user.email,
                    )
                    updatedUser?.let { tokenStore.saveUser(it) }
                    state.update { current ->
                        current.copy(
                            user = updatedUser,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error -> state.update { it.copy(errorMessage = error.message) } }
        }
    }
}
