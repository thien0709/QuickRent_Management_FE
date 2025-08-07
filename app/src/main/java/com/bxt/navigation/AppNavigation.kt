package com.bxt.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bxt.ui.screen.*
import com.bxt.ui.screen.LoginScreen
import com.bxt.viewmodel.WelcomeViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val welcomeViewModel: WelcomeViewModel = hiltViewModel()

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val isFirstTime = welcomeViewModel.dataStoreManager.isFirstTime()
        startDestination = if (isFirstTime) "welcome" else "home"

    }

    if (startDestination != null) {
        NavHost(
            navController = navController,
            startDestination = startDestination!!
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
                    onSignUpClick = {
                        navController.navigate("register") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onForgotPasswordClick = {}
                )
            }

            composable("home") {
                HomeScreen(navController)
            }

            composable("register") {
                RegisterScreen()
            }

            composable("profile") {
                ProfileScreen(navController)
            }
        }
    }
}
