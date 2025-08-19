// screen/ProfileScreen.kt (tối ưu)
package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import com.bxt.ui.components.ExpandableFab
import com.bxt.ui.components.LoadingIndicator
import com.bxt.util.FabActions
import com.bxt.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: UserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.shouldNavigateToLogin) {
        if (uiState.shouldNavigateToLogin) {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
            viewModel.onNavigationHandled()
        }
    }

    Box(Modifier.fillMaxSize().nestedScroll(TopAppBarDefaults.pinnedScrollBehavior().nestedScrollConnection)) {
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.user != null -> ProfileContent(
                user = uiState.user!!,
                onLogout = viewModel::logout,
                editProfile = { navController.navigate("edit_profile") }
            )
            uiState.error != null -> ErrorState(uiState.error!!)
        }

        ExpandableFab(actions = FabActions.profile(navController))
    }
}

@Composable
private fun ProfileContent(
    user: ApiResult<UserResponse>,
    onLogout: () -> Unit,
    editProfile: () -> Unit
) {
    val userData = (user as? ApiResult.Success)?.data
    val avatarUrl = userData?.avatarUrl ?: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop&crop=face"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier.size(120.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileItem("Tên người dùng", userData?.username)
                ProfileItem("Email", userData?.email)
                ProfileItem("Họ và tên", userData?.fullName)
                ProfileItem("Số điện thoại", userData?.phoneNumber)
            }
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) { Text("Đăng xuất") }

        Button(onClick = editProfile, modifier = Modifier.fillMaxWidth()) {
            Text("Chỉnh sửa thông tin")
        }
    }
}

@Composable
private fun ProfileItem(label: String, value: String?) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        text = value?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ErrorState(errorMessage: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
    }
}