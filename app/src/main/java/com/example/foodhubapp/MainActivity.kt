package com.example.foodhubapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.foodhubapp.navigation.AppNavGraph
import com.example.foodhubapp.ui.theme.FoodHubAppTheme

/**
 * Điểm khởi đầu (Entry Point) của ứng dụng Android.
 * Nơi khởi tạo Activity chính, cấu hình Edge-to-Edge và thiết lập Compose Theme cùng NavController.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FoodHubAppTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
