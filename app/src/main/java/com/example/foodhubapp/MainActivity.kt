package com.example.foodhubapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.foodhubapp.feature.auth.ui.RegisterScreen
import com.example.foodhubapp.feature.home.ui.HomeScreen
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

            //HomeScreen()
               // Khởi tạo NavController để quản lý điều hướng giữa các màn hình
               val navController = rememberNavController()

              //  Gọi NavGraph trung tâm của ứng dụng
               AppNavGraph(navController = navController)
            }
        }
    }
}
