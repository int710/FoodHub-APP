package com.example.foodhubapp.feature.table.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.feature.table.AdminConversation
import com.example.foodhubapp.feature.table.data.AdminConversationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminConversationUiState(
    val conversations: List<AdminConversation> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class AdminConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminConversationRepository(application.applicationContext)
    private val state = MutableStateFlow(AdminConversationUiState())
    val uiState = state.asStateFlow()

    init { refresh() }

    fun refresh() {
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                state.update { it.copy(conversations = repository.getOpenConversations(), isLoading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(isLoading = false, error = error.message ?: "Không thể tải hội thoại") }
            }
        }
    }

    fun close(id: String) {
        viewModelScope.launch {
            runCatching { repository.close(id) }
                .onSuccess { refresh() }
                .onFailure { error -> state.update { it.copy(error = error.message) } }
        }
    }
}
