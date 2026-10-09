package com.example.foodhubapp.feature.customer.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.foodhubapp.feature.customer.onboarding.model.onboardingHeroPages
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.FoodHubAppTheme
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onStartClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onSkipClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { onboardingHeroPages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.TopCenter) {
    Column(
        modifier = Modifier.fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        OnboardingTopBar(onSkipClick = onSkipClick)
        Spacer(modifier = Modifier.height(8.dp))
        OnboardingHeroPager(
            pages = onboardingHeroPages,
            pagerState = pagerState
        )
        Spacer(modifier = Modifier.height(16.dp))
        PageIndicator(
            pageCount = onboardingHeroPages.size,
            currentPage = pagerState.currentPage,
            onPageClick = { page ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(page)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OnboardingHeadline()
        Spacer(modifier = Modifier.height(20.dp))
        FeatureList()
        Spacer(modifier = Modifier.weight(1f))
        BottomActions(
            onStartClick = onStartClick,
            onLoginClick = onLoginClick
        )
        Spacer(modifier = Modifier.height(26.dp))
    }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 948)
@Composable
private fun OnboardingScreenPreview() {
    FoodHubAppTheme {
        OnboardingScreen()
    }
}
