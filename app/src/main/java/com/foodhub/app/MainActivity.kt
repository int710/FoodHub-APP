package com.foodhub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FoodHubApp(onExit = ::finish) }
    }
}

@Composable
fun FoodHubApp(onExit: () -> Unit = {}) {
    var isAdmin by remember { mutableStateOf(true) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            if (isAdmin) {
                AdminApp(onLogout = { isAdmin = false })
            } else {
                CustomerFoodHubFlow(onExit)
            }
        }
    }
}

@Composable
private fun CustomerFoodHubFlow(onExit: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    if (page == 0) {
        ScanScreen(onContinue = { page = 1 }, onBack = onExit)
    } else {
        ChatScreen(onBack = { page = 0 })
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewFoodHub() = FoodHubApp()

