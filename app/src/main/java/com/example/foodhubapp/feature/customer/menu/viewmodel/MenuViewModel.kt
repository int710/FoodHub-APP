package com.example.foodhubapp.feature.customer.menu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.feature.customer.menu.data.FoodRepository
import com.example.foodhubapp.feature.customer.menu.data.MenuCategory
import com.example.foodhubapp.feature.customer.menu.data.RemoteFoodRepository
import com.example.foodhubapp.feature.customer.menu.data.MenuSocketClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MenuUiState(val isLoading: Boolean = false, val categories: List<MenuCategory> = emptyList(), val error: String? = null)

class MenuViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: FoodRepository = RemoteFoodRepository()
) : AndroidViewModel(application) {
    private val state = MutableStateFlow(MenuUiState())
    val uiState = state.asStateFlow()
    private val menuSocket = MenuSocketClient()
    init {
        load()
        menuSocket.connect { load() }
    }

    fun load() {
        if (state.value.isLoading) return
        state.value = MenuUiState(isLoading = true)
        viewModelScope.launch {
            try { state.value = MenuUiState(categories = repository.getMenu())
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) { state.value = MenuUiState(error = foodError(error)) }
        }
    }

    override fun onCleared() {
        menuSocket.disconnect()
        super.onCleared()
    }
}
