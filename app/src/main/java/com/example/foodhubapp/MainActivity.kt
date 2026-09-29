package com.example.foodhubapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.foodhubapp.navigation.AppNavGraph
import com.example.foodhubapp.theme.FoodHubAppTheme
import com.example.foodhubapp.core.network.FoodHubSessionRefresh
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Điểm khởi đầu (Entry Point) của ứng dụng Android.
 * Nơi khởi tạo Activity chính, cấu hình Edge-to-Edge và thiết lập Compose Theme cùng NavController.
 */
class MainActivity : ComponentActivity() {
    private var pendingDeepLink by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDeepLink = intent?.data
        FoodHubSessionRefresh.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            FoodHubAppTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    deepLinkUri = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.data
    }
}
