package com.bxt.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bxt.ui.components.BottomNavItem
import com.bxt.ui.components.BottomNavigationBar
import com.bxt.ui.components.ErrorPopupManager
import com.bxt.ui.screen.*
import com.bxt.viewmodel.RentalServiceViewModel
import com.bxt.viewmodel.TransportServiceViewModel
import com.bxt.viewmodel.WelcomeViewModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val welcomeViewModel: WelcomeViewModel = hiltViewModel()

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val isFirstTime = welcomeViewModel.dataStoreManager.isFirstTime.first()
        startDestination = if (isFirstTime) "welcome" else "home"
    }

    val routesToHideBottomBar = listOf(
        "welcome",
        "login",
        "register",
        "item_detail/", // Đã sửa từ "item/" thành "item_detail/"
        "rent_item/",
        "chat_screen/", // Thêm để ẩn bottom bar trong chat
        "add_item",
        "add_transport_service"
    )

    val hideBottomBar = routesToHideBottomBar.any { routePrefix ->
        currentRoute?.startsWith(routePrefix) == true
    }

    if (startDestination != null) {
        Scaffold(
            bottomBar = {
                if (!hideBottomBar) {
                    BottomNavigationBar(
                        items = listOf(
                            BottomNavItem("Home", Icons.Default.Home, "home"),
                            BottomNavItem("Rental", Icons.Default.ShoppingCart, "category"),
                            BottomNavItem("Transport", Icons.Default.DeliveryDining, "transport_service"),
                            BottomNavItem("Chat", Icons.Default.Textsms, "chat_list"), // Đã sửa từ "chat" thành "chat_list"
                            BottomNavItem("Profile", Icons.Default.Person, "profile")
                        ),
                        currentRoute = currentRoute,
                        onItemClick = { item ->
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination!!,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                composable("welcome") {
                    WelcomeScreen(
                        onCompleteWelcome = {
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    )
                }
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onSignUpClick = { navController.navigate("register") },
                        onForgotPasswordClick = {}
                    )
                }
                composable("register") {
                    RegisterScreen(
                        onSuccess = {
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        onCategoryClick = { category ->
                            navController.navigate("category/${category.id}")
                        },
                        onItemClick = { item ->
                            // Đã sửa: navigate đến route đúng
                            navController.navigate("item_detail/${item.id}")
                        },
                        onAllCategoriesClick = {
                            navController.navigate("category")
                        },
                        onFilterClick = {}
                    )
                }
                composable("profile") {
                    ProfileScreen(navController)
                }
                composable("category") {
                    CategoryScreen(
                        navController = navController,
                        onBackClick = { navController.navigateUp() },
                        onProductClick = { productId ->
                            // Đã sửa: navigate đến route đúng
                            navController.navigate("item_detail/$productId")
                        }
                    )
                }

                // Route chính cho Item Detail
                composable(
                    route = "item_detail/{itemId}",
                    arguments = listOf(navArgument("itemId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                    ItemScreen(
                        itemId = itemId,
                        navController = navController,
                        onClickBack = { navController.popBackStack() },
                        onClickRent = { itemId, price ->
                            navController.navigate("rent_item/$itemId/$price")
                        }
                    )
                }

                composable(
                    route = "rent_item/{itemId}/{price}",
                    arguments = listOf(
                        navArgument("itemId") { type = NavType.LongType },
                        navArgument("price") { type = NavType.StringType }
                    )
                ) {
                    RentalItemScreen(
                        onClickBack = { navController.popBackStack() },
                        onRentalSuccess = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }

                composable("add_item") {
                    AddItemScreen(
                        onItemAdded = {
                            navController.navigate("home") {
                                popUpTo("add_item") { inclusive = true }
                            }
                        },
                        onUserNull = {
                            navController.navigate("login") {
                                popUpTo("add_item") { inclusive = true }
                            }
                        }
                    )
                }
                composable("rental_service") {
                    RentalServiceScreen(
                        onBackClick = { navController.popBackStack() },
                        onRentalClick = { id -> navController.navigate("rental_detail/$id") }
                    )
                }

                composable("transactions") {
                    // TransactionsScreen khi có
                }

                composable("transport_service") {
                    val viewModel: TransportServiceViewModel = hiltViewModel()
                    TransportServiceScreen(
                        navController = navController,
                        viewModel = hiltViewModel(),
                    )
                }

                composable("add_transport_service") {
                    AddTransportScreen(
                        onSubmit = {
                            navController.popBackStack()
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // Chat Routes
                composable("chat_list") {
                    ChatListScreen(
                        navController = navController,
                        viewModel = hiltViewModel() // Thêm viewModel nếu cần
                    )
                }

                composable(
                    route = "chat_screen/{otherUserId}?attachableJson={attachableJson}",
                    arguments = listOf(
                        navArgument("otherUserId") { type = NavType.StringType },
                        navArgument("attachableJson") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    ChatScreen(navController = navController)
                }
            }
            ErrorPopupManager.ErrorPopup()
        }
    }
}