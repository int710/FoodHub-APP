package com.example.foodhubapp.feature.auth.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.auth.data.AuthRepository
import com.example.foodhubapp.feature.auth.data.RemoteAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isRequestSent: Boolean = false,
    val errorMessage: String? = null
) {
    val canSend: Boolean
        get() = !isLoading && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
}

class ForgotPasswordViewModel @JvmOverloads constructor(
    application: Application,
    private val savedState: SavedStateHandle,
    private val repository: AuthRepository = RemoteAuthRepository(
        tokenStore = TokenStore(application.applicationContext)
    )
) : AndroidViewModel(application) {
    private val state = MutableStateFlow(ForgotPasswordUiState(
        email = savedState["email"] ?: "",
        isRequestSent = savedState["isRequestSent"] ?: false
    ))
    val uiState = state.asStateFlow()

    fun updateEmail(value: String) {
        if (state.value.isLoading) return
        if (value == state.value.email) return
        savedState["email"] = value
        savedState["isRequestSent"] = false
        state.update { it.copy(email = value, isRequestSent = false, errorMessage = null) }
    }

    fun sendResetLink() {
        if (state.value.isLoading) return
        if (!state.value.canSend) {
            state.update { it.copy(errorMessage = "Vui lòng nhập email hợp lệ.") }
            return
        }
        val email = state.value.email.trim()
        state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.forgotPassword(email)
                    .onSuccess {
                        savedState["isRequestSent"] = true
                        state.update { it.copy(isRequestSent = true) }
                    }
                    .onFailure { error ->
                        state.update {
                            it.copy(errorMessage = error.message?.takeIf(String::isNotBlank)
                                ?: "Không thể gửi yêu cầu. Vui lòng thử lại.")
                        }
                    }
            } finally {
                state.update { it.copy(isLoading = false) }
            }
        }
    }
}
