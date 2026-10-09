package com.example.foodhubapp.feature.admin.menu.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.feature.admin.model.AdminMenuCategory
import com.example.foodhubapp.feature.admin.model.AdminMenuCategoryInput
import com.example.foodhubapp.feature.admin.model.AdminMenuInput
import com.example.foodhubapp.feature.admin.model.AdminMenuItem
import com.example.foodhubapp.feature.admin.model.AdminFlashSaleInput
import com.example.foodhubapp.feature.admin.model.AdminVariantInput
import com.example.foodhubapp.feature.admin.menu.data.AdminMenuRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminMenuUiState(
    val items: List<AdminMenuItem> = emptyList(),
    val categories: List<AdminMenuCategory> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploading: Boolean = false,
    val busyItemId: String? = null,
    val busyCategoryId: String? = null,
    val errorMessage: String? = null,
    val message: String? = null,
)

class AdminMenuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminMenuRepository(application.applicationContext)
    private val state = MutableStateFlow(AdminMenuUiState())
    val uiState = state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (state.value.isLoading && state.value.items.isNotEmpty()) return
        state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                kotlinx.coroutines.coroutineScope {
                    val itemsDeferred = async { repository.getItems() }
                    val categoriesDeferred = async { repository.getCategories() }
                    val items = itemsDeferred.await()
                    val categories = categoriesDeferred.await()
                    state.update {
                        it.copy(
                            items = items,
                            categories = categories,
                            isLoading = false,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(isLoading = false, errorMessage = adminMenuError(error)) }
            }
        }
    }

    fun createItem(input: AdminMenuInput) = save("Đã thêm món ${input.name}") {
        repository.createItem(input)
    }

    fun uploadImage(uri: Uri, onSuccess: (String) -> Unit) {
        if (state.value.isUploading) return
        state.update { it.copy(isUploading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val url = repository.uploadImage(uri)
                state.update { it.copy(isUploading = false, message = "Đã tải ảnh lên") }
                onSuccess(url)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(isUploading = false, errorMessage = adminMenuError(error)) }
            }
        }
    }

    fun updateItem(id: String, input: AdminMenuInput) = save("Đã cập nhật món ${input.name}", id) {
        repository.updateItem(id, input)
    }

    fun toggleItem(id: String) = save("Đã cập nhật trạng thái món", id) {
        repository.toggleItem(id)
    }

    fun deleteItem(id: String) = save(null, id) {
        val message = repository.deleteItem(id)
        state.update { it.copy(message = message) }
    }

    fun createCategory(input: AdminMenuCategoryInput) = saveCategory("Đã thêm danh mục ${input.name}") {
        repository.createCategory(input)
    }

    fun updateCategory(id: String, input: AdminMenuCategoryInput) =
        saveCategory("Đã cập nhật danh mục ${input.name}", id) {
            repository.updateCategory(id, input)
        }

    fun deleteCategory(id: String) = saveCategory("Đã xóa danh mục", id) {
        repository.deleteCategory(id)
    }

    fun createFlashSale(input: AdminFlashSaleInput) = save("Đã tạo flash sale", input.itemId) {
        repository.createFlashSale(input)
    }

    fun deleteFlashSale(itemId: String) = save("Đã kết thúc flash sale", itemId) {
        repository.deleteFlashSale(itemId)
    }

    fun createVariant(itemId: String, input: AdminVariantInput) = save("Đã thêm nhóm tùy chọn", itemId) {
        repository.createVariant(itemId, input)
    }

    fun consumeMessage() {
        state.update { it.copy(message = null) }
    }

    private fun save(
        successMessage: String?,
        itemId: String? = null,
        operation: suspend () -> Unit,
    ) {
        if (state.value.isSaving || state.value.busyItemId != null) return
        state.update {
            it.copy(
                isSaving = itemId == null,
                busyItemId = itemId,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                operation()
                val items = repository.getItems()
                state.update {
                    it.copy(
                        items = items,
                        isSaving = false,
                        busyItemId = null,
                        message = successMessage ?: it.message,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update {
                    it.copy(
                        isSaving = false,
                        busyItemId = null,
                        errorMessage = adminMenuError(error),
                    )
                }
            }
        }
    }

    private fun saveCategory(
        successMessage: String,
        categoryId: String? = null,
        operation: suspend () -> Unit,
    ) {
        if (state.value.isSaving || state.value.busyItemId != null || state.value.busyCategoryId != null) return
        state.update {
            it.copy(
                isSaving = categoryId == null,
                busyCategoryId = categoryId,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                operation()
                val categories = repository.getCategories()
                val items = repository.getItems()
                state.update {
                    it.copy(
                        categories = categories,
                        items = items,
                        isSaving = false,
                        busyCategoryId = null,
                        message = successMessage,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update {
                    it.copy(
                        isSaving = false,
                        busyCategoryId = null,
                        errorMessage = adminMenuError(error),
                    )
                }
            }
        }
    }
}

private fun adminMenuError(error: Exception): String = when (error) {
    is FoodHubApiException -> when (error.statusCode) {
        401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        403 -> "Tài khoản này không có quyền quản trị thực đơn."
        else -> error.message ?: "Không thể cập nhật thực đơn."
    }
    else -> error.message ?: "Không thể kết nối máy chủ."
}
