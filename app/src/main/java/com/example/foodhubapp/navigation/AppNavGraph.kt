package com.example.foodhubapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.foodhubapp.feature.onboarding.ui.OnboardingScreen
import com.example.foodhubapp.feature.splash.ui.FoodHubSplashRoute
import com.example.foodhubapp.ui.theme.AppBackground
import com.example.foodhubapp.ui.theme.HeadingFont
import com.example.foodhubapp.ui.theme.Neutral

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = AppRoutes.Splash
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppRoutes.Splash) {
            FoodHubSplashRoute(
                onSplashFinished = {
                    navController.navigate(AppRoutes.Onboarding) {
                        popUpTo(AppRoutes.Splash) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AppRoutes.Onboarding) {
            OnboardingScreen(
                onStartClick = {
                    navController.navigate(AppRoutes.Home)
                },
                onLoginClick = {
                    navController.navigate(AppRoutes.Login)
                },
                onSkipClick = {
                    navController.navigate(AppRoutes.Home)
                }
            )
        }

        composable(AppRoutes.Home) {
            PlaceholderScreen(title = "Home")
        }

        composable(AppRoutes.Login) {
            PlaceholderScreen(title = "Login")
        }

        composable(AppRoutes.Menu) {
            PlaceholderScreen(title = "Menu")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Neutral,
            fontFamily = HeadingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
    }
}
