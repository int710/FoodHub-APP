package com.example.foodhubapp.feature.admin.chat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.feature.admin.chat.data.AdminConversationRepository
import com.example.foodhubapp.feature.admin.chat.AdminConversationSocketClient
import com.example.foodhubapp.feature.admin.chat.model.AdminConversation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminConversationUiState(
    val conversations: List<AdminConversation> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

class AdminConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminConversationRepository(application.applicationContext)
    private val socketClient = AdminConversationSocketClient(application.applicationContext)
    private val state = MutableStateFlow(AdminConversationUiState())
    val uiState = state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { socketClient.connect(::onRealtimeConversationChanged) }
    }

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

    private fun onRealtimeConversationChanged(isNew: Boolean) {
        viewModelScope.launch {
            runCatching { repository.getOpenConversations() }
                .onSuccess { conversations ->
                    state.update {
                        it.copy(
                            conversations = conversations,
                            isLoading = false,
                            message = if (isNew) "Có tin nhắn mới từ khách hàng." else "Hội thoại vừa được cập nhật.",
                        )
                    }
                }
                .onFailure { error -> state.update { it.copy(error = error.message) } }
        }
    }

    fun consumeMessage() { state.update { it.copy(message = null) } }

    override fun onCleared() {
        socketClient.disconnect()
        super.onCleared()
    }
}
