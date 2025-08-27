// screen/ProfileScreen.kt
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource // Gợi ý: Dùng cho app thực tế
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import com.bxt.ui.components.ExpandableFab
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.theme.LocalDimens
import com.bxt.util.FabActions
import com.bxt.viewmodel.UserViewModel

private const val DEFAULT_AVATAR_URL = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop&crop=face"

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

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingIndicator()

            uiState.error != null -> ErrorState(errorMessage = uiState.error!!)

            uiState.user is ApiResult.Success -> {
                val userData = (uiState.user as ApiResult.Success).data
                ProfileContent(
                    userData = userData,
                    onLogout = viewModel::logout,
                    onEditProfile = { navController.navigate("edit_profile") }
                )
            }

            uiState.user is ApiResult.Error -> {
                val errorMessage = (uiState.user as ApiResult.Error).error.message
                ErrorState(errorMessage = errorMessage)
            }
        }

        ExpandableFab(actions = FabActions.profile(navController))
    }
}

@Composable
private fun ProfileContent(
    userData: UserResponse,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = LocalDimens.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(d.pagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.sectionGap)
    ) {
        AsyncImage(
            model = userData.avatarUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_AVATAR_URL,
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(d.imageSize * 1.25f)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(d.pagePadding),
                verticalArrangement = Arrangement.spacedBy(d.rowGap)
            ) {
                ProfileItem("Tên người dùng", userData.username)
                ProfileItem("Email", userData.email)
                ProfileItem("Họ và tên", userData.fullName)
                ProfileItem("Số điện thoại", userData.phoneNumber)
            }
        }

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(d.buttonHeight),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("Đăng xuất", style = MaterialTheme.typography.titleSmall)
        }

        OutlinedButton(
            onClick = onEditProfile,
            modifier = Modifier
                .fillMaxWidth()
                .height(d.buttonHeight),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Chỉnh sửa thông tin", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun ProfileItem(
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ErrorState(
    errorMessage: String,
    modifier: Modifier = Modifier
) {
    val d = LocalDimens.current
    Box(
        modifier = modifier.fillMaxSize().padding(d.pagePadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}