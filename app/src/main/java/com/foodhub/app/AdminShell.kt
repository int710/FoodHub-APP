package com.foodhub.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

internal val AdminPrimary = Color(0xFFBA3900)
internal val AdminPrimarySoft = Color(0xFFFFEDE6)
internal val AdminBackground = Color(0xFFF7F8FC)
internal val AdminSurface = Color.White
internal val AdminBorder = Color(0xFFE5E8F0)
internal val AdminText = Color(0xFF20232A)
internal val AdminMuted = Color(0xFF6F7480)
internal val AdminGreen = Color(0xFF087F5B)
internal val AdminGreenSoft = Color(0xFFE4F6EE)
internal val AdminAmber = Color(0xFFD97706)
internal val AdminAmberSoft = Color(0xFFFFF3D6)
internal val AdminRed = Color(0xFFC7382B)
internal val AdminRedSoft = Color(0xFFFFE9E5)
internal val AdminBlue = Color(0xFF2767C5)
internal val AdminBlueSoft = Color(0xFFEAF1FD)

private val AdminColorScheme = lightColorScheme(
    primary = AdminPrimary,
    onPrimary = Color.White,
    primaryContainer = AdminPrimarySoft,
    onPrimaryContainer = AdminPrimary,
    background = AdminBackground,
    surface = AdminSurface,
    onBackground = AdminText,
    onSurface = AdminText,
    outline = AdminBorder,
    error = AdminRed,
    errorContainer = AdminRedSoft,
)

private enum class AdminTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Overview("admin/overview", "Tổng quan", Icons.Default.Dashboard),
    Orders("admin/orders", "Đơn hàng", Icons.AutoMirrored.Filled.ReceiptLong),
    Tables("admin/tables", "Bàn", Icons.Default.TableRestaurant),
    Menu("admin/menu", "Thực đơn", Icons.Default.RestaurantMenu),
    Messages("admin/messages", "Tin nhắn", Icons.Default.ChatBubbleOutline),
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun AdminApp(onLogout: () -> Unit) {
    MaterialTheme(colorScheme = AdminColorScheme) {
        val navController = rememberNavController()
        val orders = remember { mutableStateListOf<AdminOrder>().apply { addAll(AdminMockData.orders) } }
        val menuItems = remember { mutableStateListOf<AdminMenuItem>().apply { addAll(AdminMockData.menu) } }
        val conversations = remember { AdminMockData.conversations }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val route = backStackEntry?.destination?.route.orEmpty()
        val isTopLevel = AdminTab.entries.any { it.route == route }
        val title = when {
            route.startsWith("admin/order/") -> "Chi tiết đơn"
            route.startsWith("admin/chat/") -> "Hội thoại"
            route == "admin/profile" -> "Tài khoản Admin"
            else -> AdminTab.entries.firstOrNull { it.route == route }?.label ?: "FoodHub Admin"
        }

        Scaffold(
            containerColor = AdminBackground,
            topBar = {
                AdminTopBar(
                    title = title,
                    showBack = !isTopLevel,
                    onBack = navController::popBackStack,
                    onNotifications = {
                        scope.launch { snackbarHostState.showSnackbar("Không có thông báo mới") }
                    },
                    onProfile = { navController.navigate("admin/profile") },
                )
            },
            bottomBar = {
                if (isTopLevel) {
                    AdminBottomBar(
                        currentRoute = route,
                        unreadMessages = conversations.sumOf(AdminConversation::unread),
                        onSelect = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = AdminTab.Overview.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(AdminTab.Overview.route) {
                    AdminOverviewScreen(
                        orders = orders,
                        menuItems = menuItems,
                        conversations = conversations,
                        onOpenOrders = { navController.navigate(AdminTab.Orders.route) },
                        onOpenOrder = { navController.navigate("admin/order/$it") },
                        onOpenChat = { navController.navigate("admin/chat/$it") },
                        onOpenMenu = { navController.navigate(AdminTab.Menu.route) },
                    )
                }
                composable(AdminTab.Orders.route) {
                    AdminOrdersScreen(
                        orders = orders,
                        onOpenOrder = { navController.navigate("admin/order/$it") },
                        onUpdateOrder = { updated ->
                            val index = orders.indexOfFirst { it.id == updated.id }
                            if (index >= 0) orders[index] = updated
                        },
                    )
                }
                composable("admin/order/{orderId}") { entry ->
                    val order = orders.firstOrNull { it.id == entry.arguments?.getString("orderId") }
                    if (order != null) {
                        AdminOrderDetailScreen(
                            order = order,
                            onUpdate = { updated ->
                                val index = orders.indexOfFirst { it.id == updated.id }
                                if (index >= 0) orders[index] = updated
                            },
                        )
                    }
                }
                composable(AdminTab.Menu.route) {
                    AdminMenuScreen(
                        items = menuItems,
                        onToggleAvailability = { id ->
                            val index = menuItems.indexOfFirst { it.id == id }
                            if (index >= 0) {
                                menuItems[index] = menuItems[index].copy(isAvailable = !menuItems[index].isAvailable)
                            }
                        },
                        onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                    )
                }
                composable(AdminTab.Tables.route) {
                    AdminTablesScreen()
                }
                composable(AdminTab.Messages.route) {
                    AdminMessagesScreen(
                        conversations = conversations,
                        onOpenChat = { navController.navigate("admin/chat/$it") },
                    )
                }
                composable("admin/chat/{conversationId}") { entry ->
                    val conversation = conversations.firstOrNull {
                        it.id == entry.arguments?.getString("conversationId")
                    }
                    if (conversation != null) AdminChatDetailScreen(conversation)
                }
                composable("admin/profile") {
                    AdminProfileScreen(onLogout = onLogout)
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun AdminTopBar(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                }
            }
        },
        actions = {
            IconButton(onClick = onNotifications) {
                BadgedBox(badge = { Badge { Text("3") } }) {
                    Icon(Icons.Default.Notifications, "Thông báo")
                }
            }
            Box(
                Modifier.padding(end = 12.dp).size(36.dp).background(AdminPrimary, CircleShape)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center,
            ) {
                Text("AD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminSurface),
    )
}

@Composable
private fun AdminBottomBar(
    currentRoute: String,
    unreadMessages: Int,
    onSelect: (AdminTab) -> Unit,
) {
    NavigationBar(containerColor = AdminSurface) {
        AdminTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    if (tab == AdminTab.Messages && unreadMessages > 0) {
                        BadgedBox(badge = { Badge { Text(unreadMessages.toString()) } }) {
                            Icon(tab.icon, tab.label)
                        }
                    } else {
                        Icon(tab.icon, tab.label)
                    }
                },
                label = { Text(tab.label, fontSize = 11.sp) },
            )
        }
    }
}
