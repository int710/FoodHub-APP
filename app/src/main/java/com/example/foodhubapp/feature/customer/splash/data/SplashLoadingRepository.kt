package com.example.foodhubapp.feature.customer.splash.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds

data class SplashLoadingProgress(
    val message: String,
    val progressPercent: Int
)

interface SplashLoadingRepository {
    fun observeMenuSyncProgress(): Flow<SplashLoadingProgress>
}

class LocalSplashLoadingRepository : SplashLoadingRepository {
    override fun observeMenuSyncProgress(): Flow<SplashLoadingProgress> = flow {
        val steps = listOf(
            SplashLoadingProgress("Đang khởi động FoodHub...", 20),
            SplashLoadingProgress("Đang chuẩn bị giao diện...", 55),
            SplashLoadingProgress("Đang hoàn tất thiết lập...", 85),
            SplashLoadingProgress("Sẵn sàng", 100)
        )

        for (step in steps) {
            emit(step)
            delay(650.milliseconds)
        }
    }
}
