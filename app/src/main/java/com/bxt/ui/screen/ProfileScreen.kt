package com.bxt.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import com.bxt.ui.components.LoadingIndicator
import com.bxt.viewmodel.UserViewModel

/* ===================== ProfileScreen — NO SCAFFOLD ===================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: UserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.shouldNavigateToLogin) {
        if (uiState.shouldNavigateToLogin) {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
            viewModel.onNavigationHandled()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        Column(Modifier.fillMaxSize()) {

             when {
                uiState.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { LoadingIndicator() }

                uiState.user != null -> ProfileContent(
                    user = uiState.user!!,
                    onLogout = { viewModel.logout() },
                    editProfile = { navController.navigate("edit_profile") }
                )

                uiState.error != null -> ErrorState(errorMessage = uiState.error!!)
            }
        }

        // Speed-dial FAB (không cần Scaffold)
        ExpandableFab(
            expanded = menuOpen,
            onMainClick = { menuOpen = !menuOpen },
            onDismiss = { menuOpen = false },
            actions = listOf(
                SpeedAction(
                    label = "Profile",
                    icon = { AvatarDot(url = (uiState.user as? ApiResult.Success)?.data?.avatarUrl) },
                    onClick = { navController.navigate("profile_detail") }
                ),
                SpeedAction(
                    label = "Task Center",
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    onClick = { navController.navigate("task_center") }
                ),
                SpeedAction(
                    label = "Creator Center",
                    icon = { Icon(Icons.Filled.GridView, contentDescription = null) },
                    onClick = { navController.navigate("creator_center") }
                ),
                SpeedAction(
                    label = "Notification",
                    icon = { NotificationBell(badge = 1) },
                    onClick = { navController.navigate("notifications") }
                ),
                SpeedAction(
                    label = "Post",
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    accent = true,
                    onClick = { navController.navigate("create_post") }
                ),
            )
        )
    }
}
data class SpeedAction(
    val label: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val accent: Boolean = false
)

@Composable
private fun ExpandableFab(
    expanded: Boolean,
    onMainClick: () -> Unit,
    onDismiss: () -> Unit,
    actions: List<SpeedAction>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp, end = 24.dp), // tránh đè bottom bar
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + slideInVertically(
                        tween(180, easing = FastOutSlowInEasing),
                        initialOffsetY = { it / 2 }
                    ),
                    exit = fadeOut() + slideOutVertically(
                        tween(140),
                        targetOffsetY = { it / 2 }
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        actions.forEach { a ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = a.label,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                FilledIconButton(
                                    onClick = { onDismiss(); a.onClick() },
                                    shape = CircleShape,
                                    colors = if (a.accent)
                                        IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    else IconButtonDefaults.filledIconButtonColors()
                                ) { a.icon() }
                            }
                        }

                        FilledIconButton(
                            onClick = onDismiss,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) { Icon(Icons.Filled.Close, contentDescription = "Đóng") }
                    }
                }

                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 45f else 0f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                )
                FloatingActionButton(onClick = onMainClick) {
                    Icon(Icons.Filled.Add, contentDescription = "Mở menu", modifier = Modifier.rotate(rotation))
                }
            }
        }
    }
}

@Composable
private fun AvatarDot(url: String?) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(24.dp).clip(CircleShape)
        )
    } else {
        Icon(Icons.Filled.Person, contentDescription = null)
    }
}

@Composable
private fun NotificationBell(badge: Int) {
    Box(contentAlignment = Alignment.TopEnd) {
        Icon(Icons.Filled.Notifications, contentDescription = null)
        if (badge > 0) {
            Box(
                Modifier
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badge > 9) "9+" else badge.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

/* ===================== Nội dung hồ sơ & trạng thái lỗi ===================== */

@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier,
    user: ApiResult<UserResponse>,
    onLogout: () -> Unit,
    editProfile: () -> Unit
) {
    val userData = (user as? ApiResult.Success)?.data
    val avatarUrl = userData?.avatarUrl
        ?: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop&crop=face"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Profile",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
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

        Button(
            onClick = editProfile,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Chỉnh sửa thông tin") }
    }
}

@Composable
private fun ProfileItem(label: String, value: String?) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (value.isNullOrBlank()) "Chưa cập nhật" else value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorState(
    modifier: Modifier = Modifier,
    errorMessage: String
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
    }
}
