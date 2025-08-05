package com.bxt.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.ui.screen.HomeScreen
import com.bxt.ui.screen.LoginScreen
import com.bxt.ui.screen.ProfileScreen
import com.bxt.ui.screen.RegisterScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
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
                onForgotPasswordClick = {})

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