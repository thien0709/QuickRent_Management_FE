package com.bxt.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.bxt.ui.screen.*
import com.bxt.viewmodel.WelcomeViewModel
import kotlinx.coroutines.flow.first

@RequiresApi(Build.VERSION_CODES.O)
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

    val noBottomBarRoutes = listOf("welcome", "login", "register")
    val hideBottomBar = currentRoute in noBottomBarRoutes ||
            currentRoute?.startsWith("item/") == true

    if (startDestination != null) {
        Scaffold(
            bottomBar = {
                if (!hideBottomBar) {
                    BottomNavigationBar(
                        items = listOf(
                            BottomNavItem("Home", Icons.Default.Home, "home"),
                            BottomNavItem("Take on Rent", Icons.Default.ShoppingCart, "category"),
                            BottomNavItem("Give on Rent", Icons.Default.AddCircle,"add_item"),
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
                Modifier.padding(innerPadding)
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
                composable("register") { RegisterScreen() }


                composable("home") {
                    HomeScreen(
                        onCategoryClick = { category ->
                            navController.navigate("category/${category.id}")
                        },
                        onItemClick = { item ->
                            navController.navigate("item/${item.id}")
                        },
                        onAllCategoriesClick = {
                            navController.navigate("category")
                        },
                        onFilterClick = {}
                    )
                }
                composable("profile") { ProfileScreen(navController) }
                composable("category") {
                    CategoryScreen(
                        onBackClick = { navController.navigateUp() },
                        onProductClick = { productId ->
                            navController.navigate("item/$productId")
                        }
                    )
                }
                composable(
                    route = "item/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getLong("id") ?: error("Missing item id")
                    ItemScreen(
                        itemId = itemId,
                        onClickBack = { navController.popBackStack() },
                        onClickOwner = { ownerId ->  },
                        onClickRent = { id -> /* xử lý thuê với id */ }

                    )
                }
                composable("add_item") {
                    AddItemScreen(
                        onItemAdded = {
                            navController.navigate("home") {
                                popUpTo("add_item") { inclusive = true }
                            }
                        }
                    )
                }


            }
            ErrorPopupManager.ErrorPopup()
        }
    }
}
