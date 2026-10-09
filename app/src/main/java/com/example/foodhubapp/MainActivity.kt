package com.example.foodhubapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.foodhubapp.core.payment.VnPayReturn
import com.example.foodhubapp.core.payment.toVnPayReturnOrNull
import com.example.foodhubapp.navigation.AppNavGraph
import com.example.foodhubapp.navigation.AppRoutes
import com.example.foodhubapp.theme.FoodHubAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.foodhubapp.feature.table.data.TableSessionStore

/**
 * Điểm khởi đầu (Entry Point) của ứng dụng Android.
 * Nơi khởi tạo Activity chính, cấu hình Edge-to-Edge và thiết lập Compose Theme cùng NavController.
 */
class MainActivity : ComponentActivity() {
    private val paymentReturn = MutableStateFlow<VnPayReturn?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()

        setContent {
            FoodHubAppTheme {
                val navController = rememberNavController()
                val currentPaymentReturn by paymentReturn.collectAsStateWithLifecycle()
                AppNavGraph(
                    navController = navController,
                    startDestination = when {
                        currentPaymentReturn == null -> AppRoutes.Splash
                        TableSessionStore(applicationContext).current() != null -> AppRoutes.Cart
                        else -> AppRoutes.Orders
                    },
                    paymentReturn = currentPaymentReturn,
                    onPaymentReturnConsumed = { consumed ->
                        if (paymentReturn.value == consumed) paymentReturn.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.toVnPayReturnOrNull()?.let { paymentReturn.value = it }
    }
}
