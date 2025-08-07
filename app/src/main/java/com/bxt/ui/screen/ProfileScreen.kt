package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bxt.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(navController: NavController) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val user by viewModel.user.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(error) {
        if (error != null && error!!.contains("Không có token")) {
            navController.navigate("login") {
                popUpTo("profile") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (user != null) {
            Text("User: ${user!!.toString()}", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { viewModel.logout(); navController.navigate("login") }) {
                Text("Logout")
            }
        } else if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}