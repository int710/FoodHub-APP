package com.example.foodhubapp.feature.menu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.feature.menu.data.*
import com.example.foodhubapp.feature.menu.ui.FoodCartSelection
import com.example.foodhubapp.feature.menu.ui.FoodDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import org.json.JSONException
import com.foodhub.app.TableSessionStore
import com.example.foodhubapp.feature.cart.data.CartContextStore
import com.example.foodhubapp.feature.cart.data.CartType

data class FoodDetailUiState(
    val isLoading: Boolean = false,
    val food: FoodDetail? = null,
    val error: String? = null,
    val isAdding: Boolean = false,
    val message: String? = null,
    val requiresLogin: Boolean = false,
    val requiresTableScan: Boolean = false
)

class FoodDetailViewModel @JvmOverloads constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val repository: FoodRepository = RemoteFoodRepository(
        tableToken = { TableSessionStore(application.applicationContext).current()?.tableToken },
        accessToken = TokenStore(application.applicationContext)::getAccessToken,
        accountCartType = { CartContextStore(application.applicationContext).currentType() }
    )
) : AndroidViewModel(application) {
    private val foodId = savedStateHandle.get<String>("foodId").orEmpty()
    private val tableSessionStore = TableSessionStore(application.applicationContext)
    private val state = MutableStateFlow(FoodDetailUiState())
    val uiState = state.asStateFlow()

    init { loadFood() }

    /** Đọc foodId từ route rồi GET `/menu/item/{id}` qua FoodRepository. */
    fun loadFood() {
        if (state.value.isLoading || state.value.isAdding) return
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                check(foodId.isNotBlank()) { "Không tìm thấy ID món ăn." }
                val food = repository.getFood(foodId)
                state.update { it.copy(food = food) }
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) {
                state.update { it.copy(error = foodError(error)) }
            } finally {
                state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun addToCart(selection: FoodCartSelection) {
        val current = state.value
        if (current.isAdding || current.isLoading) return
        val food = current.food ?: return
        val ids = selection.variantOptionIds
        // Xác thực theo FoodDetail đang hiển thị trước khi tạo request POST add.
        val valid = food.available && selection.menuItemId == food.id && selection.quantity in 1..99 &&
            selection.note.length <= 255 && ids.distinct().size == ids.size &&
            ids.all { id -> food.groups.any { group -> group.options.any { it.id == id } } } &&
            food.groups.all { group ->
                val count = group.options.count { it.id in ids }
                (!group.required || count > 0) && (group.multiple || count <= 1)
            }
        if (!valid) {
            state.update { it.copy(message = "Vui lòng kiểm tra số lượng và tùy chọn bắt buộc.") }
            return
        }
        state.update { it.copy(isAdding = true, message = null, requiresLogin = false, requiresTableScan = false) }
        viewModelScope.launch {
            try {
                // Repository tự chọn X-Table-Token/DINE_IN hoặc Bearer/TAKEAWAY theo phiên hiện tại.
                repository.addToCart(selection)
                val type = CartContextStore(getApplication<Application>().applicationContext).currentType()
                val label = when (type) {
                    CartType.DINE_IN -> "tại bàn"
                    CartType.TAKEAWAY -> "mang về"
                    CartType.DELIVERY -> "giao tận nơi"
                }
                state.update { it.copy(message = "Đã thêm món vào giỏ $label.") }
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) {
                if (error is FoodHubApiException && error.statusCode == 401 && tableSessionStore.current() != null) {
                    tableSessionStore.clear()
                    state.update { it.copy(requiresTableScan = true) }
                } else if (error is LoginRequiredException || (error is FoodHubApiException && error.statusCode == 401)) {
                    state.update { it.copy(requiresLogin = true) }
                } else state.update { it.copy(message = foodError(error)) }
            } finally {
                state.update { it.copy(isAdding = false) }
            }
        }
    }

    fun dismissLogin() { state.update { it.copy(requiresLogin = false) } }
    fun dismissTableScan() { state.update { it.copy(requiresTableScan = false) } }
    fun consumeMessage() { state.update { it.copy(message = null) } }
}

internal fun foodError(error: Exception): String = when (error) {
    is SocketTimeoutException -> "Máy chủ phản hồi quá lâu. Vui lòng thử lại."
    is IOException -> "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại."
    is JSONException -> "Dữ liệu món ăn không hợp lệ. Vui lòng thử lại sau."
    is FoodHubApiException -> if (error.statusCode == 404) "Món ăn không còn tồn tại." else error.message.orEmpty()
    else -> error.message ?: "Không thể tải dữ liệu. Vui lòng thử lại."
}
