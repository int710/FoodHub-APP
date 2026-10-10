package com.example.foodhubapp.feature.customer.splash.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.customer.splash.viewmodel.FoodHubSplashViewModel
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.FoodHubAppTheme
import com.example.foodhubapp.theme.Primary
import kotlinx.coroutines.delay

private const val LOGO_DISPLAY_MILLIS = 650L

@Composable
fun FoodHubSplashRoute(
    onSplashFinished: (isAuthenticated: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodHubSplashViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isAuthenticated) {
        val isAuthenticated = uiState.isAuthenticated ?: return@LaunchedEffect
        delay(LOGO_DISPLAY_MILLIS)
        onSplashFinished(isAuthenticated)
    }

    FoodHubSplashScreen(modifier = modifier)
}

@Composable
fun FoodHubSplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.foodhub_logo),
            contentDescription = "Logo FoodHub",
            modifier = Modifier
                .size(120.dp)
                .shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = Primary.copy(alpha = 0.28f),
                    spotColor = Primary.copy(alpha = 0.32f)
                )
                .clip(RoundedCornerShape(30.dp))
        )
    }
}

@Preview(showBackground = true, widthDp = 430, heightDp = 932)
@Composable
private fun FoodHubSplashScreenPreview() {
    FoodHubAppTheme {
        FoodHubSplashScreen()
    }
}
