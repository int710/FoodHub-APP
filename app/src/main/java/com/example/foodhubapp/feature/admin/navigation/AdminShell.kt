package com.example.foodhubapp.feature.admin.navigation

import com.example.foodhubapp.feature.admin.order.data.AdminOrderSocketClient
import com.example.foodhubapp.feature.admin.model.AdminPaymentMethod
import com.example.foodhubapp.feature.admin.table.ui.AdminTablesScreen

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.feature.admin.chat.ui.AdminChatDetailScreen
import com.example.foodhubapp.feature.admin.menu.ui.AdminMenuScreen
import com.example.foodhubapp.feature.admin.chat.ui.AdminMessagesScreen
import com.example.foodhubapp.feature.admin.order.ui.AdminOrderDetailScreen
import com.example.foodhubapp.feature.admin.order.ui.AdminOrdersScreen
import com.example.foodhubapp.feature.admin.overview.ui.AdminOverviewScreen
import com.example.foodhubapp.feature.admin.profile.ui.AdminProfileScreen
import com.example.foodhubapp.feature.admin.menu.viewmodel.AdminMenuViewModel
import com.example.foodhubapp.feature.admin.order.viewmodel.AdminOrderViewModel
import com.example.foodhubapp.feature.admin.chat.model.AdminConversation
import com.example.foodhubapp.feature.admin.chat.viewmodel.AdminConversationViewModel
import com.example.foodhubapp.feature.shared.notification.ui.NotificationScreen
import com.example.foodhubapp.feature.shared.notification.viewmodel.NotificationViewModel
import com.example.foodhubapp.core.datastore.TokenStore

internal val AdminPrimary = Color(0xFFA73400)
internal val AdminPrimarySoft = Color(0xFFFFDBD0)
internal val AdminBackground = Color(0xFFF8F9FF)
internal val AdminSurface = Color.White
internal val AdminBorder = Color(0xFFE5E8F0)
internal val AdminText = Color(0xFF181C22)
internal val AdminMuted = Color(0xFF5B4138)
internal val AdminGreen = Color(0xFF006947)
internal val AdminGreenSoft = Color(0xFFD9FBEA)
internal val AdminAmber = Color(0xFF6A4800)
internal val AdminAmberSoft = Color(0xFFFFDEAC)
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
        val context = LocalContext.current
        val adminUser by remember(context) { TokenStore(context.applicationContext) }
            .user.collectAsStateWithLifecycle(initialValue = null)
        val orderViewModel: AdminOrderViewModel = viewModel()
        val orderState by orderViewModel.uiState.collectAsStateWithLifecycle()
        val menuViewModel: AdminMenuViewModel = viewModel()
        val menuState by menuViewModel.uiState.collectAsStateWithLifecycle()
        val notificationViewModel: NotificationViewModel = viewModel()
        val notificationState by notificationViewModel.uiState.collectAsStateWithLifecycle()
        val conversationViewModel: AdminConversationViewModel = viewModel()
        val conversationState by conversationViewModel.uiState.collectAsStateWithLifecycle()
        val conversations = conversationState.conversations
        val snackbarHostState = remember { SnackbarHostState() }
        val orderSocket = remember { AdminOrderSocketClient(context) }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val route = backStackEntry?.destination?.route.orEmpty()
        val isTopLevel = AdminTab.entries.any { it.route == route }
        val title = when {
            route.startsWith("admin/order/") -> "Chi tiết đơn"
            route.startsWith("admin/chat/") -> "Hội thoại"
            route == "admin/profile" -> "Tài khoản Admin"
            route == "admin/notifications" -> "Thông báo"
            else -> AdminTab.entries.firstOrNull { it.route == route }?.label ?: "FoodHub Admin"
        }

        LaunchedEffect(orderSocket) { orderSocket.connect(orderViewModel::refresh) }
        DisposableEffect(orderSocket) { onDispose(orderSocket::disconnect) }

        LaunchedEffect(menuState.message) {
            menuState.message?.let {
                snackbarHostState.showSnackbar(it)
                menuViewModel.consumeMessage()
            }
        }

        LaunchedEffect(menuState.errorMessage) {
            menuState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
        }
        LaunchedEffect(orderState.message) {
            orderState.message?.let {
                snackbarHostState.showSnackbar(it)
                orderViewModel.consumeMessage()
            }
        }
        LaunchedEffect(orderState.error) {
            orderState.error?.let { snackbarHostState.showSnackbar(it) }
        }

        Scaffold(
            containerColor = AdminBackground,
            topBar = {
                AdminTopBar(
                    title = title,
                    showBack = !isTopLevel,
                    unreadNotifications = notificationState.unreadCount,
                    onBack = navController::popBackStack,
                    onNotifications = {
                        notificationViewModel.refresh()
                        navController.navigate("admin/notifications") { launchSingleTop = true }
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
                            if (tab == AdminTab.Messages) conversationViewModel.refresh()
                            if (tab == AdminTab.Orders) orderViewModel.refresh()
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
                        orders = orderState.orders,
                        menuItems = menuState.items,
                        conversations = conversations,
                        onOpenOrders = { navController.navigate(AdminTab.Orders.route) },
                        onOpenOrder = { navController.navigate("admin/order/$it") },
                        onOpenChat = { navController.navigate("admin/chat/$it") },
                        onOpenMenu = { navController.navigate(AdminTab.Menu.route) },
                    )
                }
                composable(AdminTab.Orders.route) {
                    AdminOrdersScreen(
                        orders = orderState.orders,
                        onOpenOrder = { navController.navigate("admin/order/$it") },
                        busyId = orderState.busyId,
                        onConfirm = { order ->
                            if (order.paymentMethod == AdminPaymentMethod.CASH && !order.paid) {
                                orderViewModel.confirmCash(order.id)
                            } else orderViewModel.confirm(order.id)
                        },
                        onReject = { order, reason -> orderViewModel.reject(order.id, reason) },
                        onRefresh = orderViewModel::refresh,
                    )
                }
                composable("admin/order/{orderId}") { entry ->
                    val order = orderState.orders.firstOrNull { it.id == entry.arguments?.getString("orderId") }
                    if (order != null) {
                        AdminOrderDetailScreen(
                            order = order,
                            busyId = orderState.busyId,
                            onConfirm = {
                                if (it.paymentMethod == AdminPaymentMethod.CASH && !it.paid) {
                                    orderViewModel.confirmCash(it.id)
                                } else orderViewModel.confirm(it.id)
                            },
                            onUpdateItem = orderViewModel::updateItem,
                            onServe = { orderViewModel.serve(it.id) },
                            onComplete = { orderViewModel.complete(it.id) },
                        )
                    }
                }
                composable(AdminTab.Menu.route) {
                    AdminMenuScreen(
                        items = menuState.items,
                        categories = menuState.categories,
                        isLoading = menuState.isLoading,
                        isSaving = menuState.isSaving,
                        isUploading = menuState.isUploading,
                        busyItemId = menuState.busyItemId,
                        busyCategoryId = menuState.busyCategoryId,
                        errorMessage = menuState.errorMessage,
                        onRefresh = menuViewModel::refresh,
                        onCreate = menuViewModel::createItem,
                        onUpdate = menuViewModel::updateItem,
                        onDelete = menuViewModel::deleteItem,
                        onToggleAvailability = menuViewModel::toggleItem,
                        onCreateCategory = menuViewModel::createCategory,
                        onUpdateCategory = menuViewModel::updateCategory,
                        onDeleteCategory = menuViewModel::deleteCategory,
                        onCreateFlashSale = menuViewModel::createFlashSale,
                        onDeleteFlashSale = menuViewModel::deleteFlashSale,
                        onUploadImage = menuViewModel::uploadImage,
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
                    if (conversation != null) {
                        AdminChatDetailScreen(
                            conversation = conversation,
                            onClose = {
                                conversationViewModel.close(conversation.id)
                                navController.popBackStack()
                            },
                        )
                    }
                }
                composable("admin/profile") {
                    AdminProfileScreen(user = adminUser, onLogout = onLogout)
                }
                composable("admin/notifications") {
                    NotificationScreen(
                        state = notificationState,
                        onBack = navController::popBackStack,
                        onRefresh = notificationViewModel::refresh,
                        onUnreadOnlyChange = notificationViewModel::setUnreadOnly,
                        onNotificationClick = { notification ->
                            if (!notification.isRead) notificationViewModel.markAsRead(notification)
                            notification.orderId?.let { orderId ->
                                navController.navigate("admin/order/$orderId")
                            }
                        },
                        onMarkAllRead = notificationViewModel::markAllAsRead,
                        showHeader = false,
                    )
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
    unreadNotifications: Int,
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
                if (unreadNotifications > 0) {
                    BadgedBox(badge = {
                        Badge { Text(if (unreadNotifications > 99) "99+" else unreadNotifications.toString()) }
                    }) {
                        Icon(Icons.Default.Notifications, "Thông báo")
                    }
                } else Icon(Icons.Default.Notifications, "Thông báo")
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
