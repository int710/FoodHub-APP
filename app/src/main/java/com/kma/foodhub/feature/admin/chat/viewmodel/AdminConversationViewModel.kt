package com.kma.foodhub.feature.admin.chat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kma.foodhub.feature.admin.chat.data.AdminConversationRepository
import com.kma.foodhub.feature.admin.chat.AdminConversationSocketClient
import com.kma.foodhub.feature.admin.chat.model.AdminConversation
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
                state.update { it.copy(conversations = repository.getConversations(), isLoading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(isLoading = false, error = error.message ?: "Không thể tải hội thoại") }
            }
        }
    }

    private fun onRealtimeConversationChanged(isNew: Boolean) {
        viewModelScope.launch {
            runCatching { repository.getConversations() }
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

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteConversation(id) }
                .onSuccess {
                    state.update { current -> current.copy(
                        conversations = current.conversations.filterNot { it.id == id },
                        message = "Đã xóa toàn bộ cuộc trò chuyện.",
                    ) }
                }
                .onFailure { error -> state.update { it.copy(error = error.message ?: "Không thể xóa cuộc trò chuyện") } }
        }
    }

    override fun onCleared() {
        socketClient.disconnect()
    }
}
