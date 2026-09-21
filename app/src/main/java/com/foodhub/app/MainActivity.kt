package com.foodhub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FoodHubApp() }
    }
}

@Composable
fun FoodHubApp() {
    var page by remember { mutableStateOf(0) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            if (page == 0) {
                ScanScreen(onContinue = { page = 1 })
            } else {
                ChatScreen(onBack = { page = 0 })
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewFoodHub() = FoodHubApp()

