package com.example.foodhubapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.foodhubapp.navigation.AppNavGraph
import com.example.foodhubapp.ui.theme.FoodHubAppTheme

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
