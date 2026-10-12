package com.kma.foodhub.feature.admin.management.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kma.foodhub.feature.admin.management.data.AdminManagementRepository
import com.kma.foodhub.feature.admin.management.model.AdminReview
import com.kma.foodhub.feature.admin.management.model.SystemSetting
import com.kma.foodhub.feature.admin.management.model.AdminVoucher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminManagementState(
    val reviews: List<AdminReview> = emptyList(),
    val settings: SystemSetting = SystemSetting(),
    val isLoadingReviews: Boolean = false,
    val isLoadingSettings: Boolean = false,
    val isSaving: Boolean = false,
    val busyReviewId: String? = null,
    val error: String? = null,
    val message: String? = null,
    val vouchers: List<AdminVoucher> = emptyList(),
    val isLoadingVouchers: Boolean = false,
    val busyVoucherId: String? = null,
)

class AdminManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminManagementRepository(application)
    private val mutableState = MutableStateFlow(AdminManagementState())
    val state = mutableState.asStateFlow()

    fun loadReviews(rating: Int? = null, search: String? = null) = launch(
        before = { it.copy(isLoadingReviews = true, error = null) },
        action = { repository.getReviews(rating, search?.trim()?.takeIf(String::isNotBlank)) },
        success = { current, reviews -> current.copy(reviews = reviews, isLoadingReviews = false) },
        failure = { current, error -> current.copy(isLoadingReviews = false, error = "Không thể tải đánh giá: ${error.errorText()}") },
    )

    fun deleteReview(id: String) = launch(
        before = { it.copy(busyReviewId = id, error = null) },
        action = { repository.deleteReview(id) },
        success = { current, _ -> current.copy(reviews = current.reviews.filterNot { it.id == id }, busyReviewId = null, message = "Đã xóa đánh giá") },
        failure = { current, error -> current.copy(busyReviewId = null, error = "Không thể xóa đánh giá: ${error.errorText()}") },
    )

    fun loadSettings() = launch(
        before = { it.copy(isLoadingSettings = true, error = null) },
        action = repository::getSettings,
        success = { current, settings -> current.copy(settings = settings, isLoadingSettings = false) },
        failure = { current, error -> current.copy(isLoadingSettings = false, error = "Không thể tải cấu hình: ${error.errorText()}") },
    )

    fun updateSettings(value: SystemSetting) = launch(
        before = { it.copy(isSaving = true, error = null) },
        action = { repository.updateSettings(value) },
        success = { current, settings -> current.copy(settings = settings, isSaving = false, message = "Đã lưu cấu hình hệ thống") },
        failure = { current, error -> current.copy(isSaving = false, error = "Không thể lưu cấu hình: ${error.errorText()}") },
    )

    fun loadVouchers() = launch(
        before = { it.copy(isLoadingVouchers = true, error = null) }, action = repository::getVouchers,
        success = { current, vouchers -> current.copy(vouchers = vouchers, isLoadingVouchers = false) },
        failure = { current, error -> current.copy(isLoadingVouchers = false, error = "Không thể tải voucher: ${error.errorText()}") },
    )

    fun saveVoucher(voucher: AdminVoucher) = launch(
        before = { it.copy(busyVoucherId = voucher.id.ifBlank { "new" }, error = null) },
        action = { repository.saveVoucher(voucher) },
        success = { current, saved -> current.copy(
            vouchers = (current.vouchers.filterNot { it.id == saved.id } + saved).sortedByDescending { it.isActive },
            busyVoucherId = null, message = "Đã lưu voucher ${saved.code}") },
        failure = { current, error -> current.copy(busyVoucherId = null, error = "Không thể lưu voucher: ${error.errorText()}") },
    )

    fun deleteVoucher(id: String) = launch(
        before = { it.copy(busyVoucherId = id, error = null) }, action = { repository.deleteVoucher(id) },
        success = { current, _ -> current.copy(vouchers = current.vouchers.filterNot { it.id == id }, busyVoucherId = null, message = "Đã xóa voucher") },
        failure = { current, error -> current.copy(busyVoucherId = null, error = "Không thể xóa voucher: ${error.errorText()}") },
    )

    fun consumeNotice() = mutableState.update { it.copy(error = null, message = null) }

    private fun <T> launch(
        before: (AdminManagementState) -> AdminManagementState,
        action: suspend () -> T,
        success: (AdminManagementState, T) -> AdminManagementState,
        failure: (AdminManagementState, Throwable) -> AdminManagementState,
    ) {
        mutableState.update(before)
        viewModelScope.launch {
            try { val value = action(); mutableState.update { success(it, value) } }
            catch (error: CancellationException) { throw error }
            catch (error: Throwable) { mutableState.update { failure(it, error) } }
        }
    }
}

private fun Throwable.errorText() = message ?: "Vui lòng thử lại"
