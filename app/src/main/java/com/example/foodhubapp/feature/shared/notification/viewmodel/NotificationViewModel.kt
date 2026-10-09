package com.example.foodhubapp.feature.shared.notification.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.feature.shared.notification.data.FoodHubNotification
import com.example.foodhubapp.feature.shared.notification.data.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val notifications: List<FoodHubNotification> = emptyList(),
    val unreadCount: Int = 0,
    val unreadOnly: Boolean = false,
    val isLoading: Boolean = true,
    val busyId: String? = null,
    val errorMessage: String? = null,
    val requiresLogin: Boolean = false
)

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = TokenStore(application.applicationContext)
    private val repository = NotificationRepository(application.applicationContext)
    private val state = MutableStateFlow(NotificationUiState())
    val uiState = state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (state.value.isLoading && state.value.notifications.isNotEmpty()) return
        state.update { it.copy(isLoading = true, errorMessage = null, requiresLogin = false) }
        viewModelScope.launch {
            if (tokenStore.getAccessToken().isNullOrBlank()) {
                state.update { it.copy(isLoading = false, requiresLogin = true) }
                return@launch
            }
            try {
                coroutineScope {
                    val notificationsDeferred = async { repository.getNotifications(state.value.unreadOnly) }
                    val unreadCountDeferred = async { repository.getUnreadCount() }
                    val notifications = notificationsDeferred.await()
                    val unreadCount = unreadCountDeferred.await()
                    state.update {
                        it.copy(
                            notifications = notifications,
                            unreadCount = unreadCount,
                            isLoading = false,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            }
        }
    }

    fun setUnreadOnly(value: Boolean) {
        if (state.value.unreadOnly == value) return
        state.update { it.copy(unreadOnly = value, notifications = emptyList(), isLoading = false) }
        refresh()
    }

    fun markAsRead(notification: FoodHubNotification) {
        if (notification.isRead || state.value.busyId != null) return
        state.update { it.copy(busyId = notification.id, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.markAsRead(notification.id)
                state.update {
                    it.copy(
                        notifications = if (it.unreadOnly) {
                            it.notifications.filterNot { item -> item.id == notification.id }
                        } else {
                            it.notifications.map { item ->
                                if (item.id == notification.id) item.copy(readAt = "now") else item
                            }
                        },
                        unreadCount = (it.unreadCount - 1).coerceAtLeast(0),
                        busyId = null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            }
        }
    }

    fun markAllAsRead() {
        if (state.value.unreadCount == 0 || state.value.busyId != null) return
        state.update { it.copy(busyId = MARK_ALL, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.markAllAsRead()
                state.update {
                    it.copy(
                        notifications = if (it.unreadOnly) emptyList() else {
                            it.notifications.map { item -> item.copy(readAt = item.readAt ?: "now") }
                        },
                        unreadCount = 0,
                        busyId = null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleError(error)
            }
        }
    }

    private fun handleError(error: Exception) {
        if ((error is FoodHubApiException && error.statusCode == 401) || error.message?.contains("đăng nhập", ignoreCase = true) == true) {
            state.update {
                it.copy(
                    isLoading = false,
                    busyId = null,
                    requiresLogin = true,
                    errorMessage = notificationError(error)
                )
            }
        } else {
            state.update { it.copy(isLoading = false, busyId = null, errorMessage = notificationError(error)) }
        }
    }

    fun dismissLogin() { state.update { it.copy(requiresLogin = false) } }

    companion object {
        const val MARK_ALL = "mark-all"
    }
}

private fun notificationError(error: Exception): String = when (error) {
    is FoodHubApiException -> when (error.statusCode) {
        401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        else -> error.message ?: "Không thể tải thông báo."
    }
    else -> error.message ?: "Không thể kết nối máy chủ."
}
