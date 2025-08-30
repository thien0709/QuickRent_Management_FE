package com.bxt.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bxt.ui.state.ChatListUiState
import com.bxt.viewmodel.ChatListViewModel
import com.bxt.viewmodel.ChatThreadUi
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tin nhắn") }) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding)
        ) {
            ChatListContent(
                state = uiState,
                onThreadClick = { userId ->
                    navController.navigate("chat_screen/$userId")
                }
            )
        }
    }
}

@Composable
private fun ChatListContent(
    state: ChatListUiState,
    modifier: Modifier = Modifier,
    onThreadClick: (String) -> Unit
) {
    (state as? ChatListUiState.Error)?.message?.let {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            AssistChip(onClick = {}, label = { Text(it) })
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is ChatListUiState.Loading -> ListLoadingState()
            is ChatListUiState.Success -> {
                if (state.threads.isEmpty()) {
                    EmptyState(message = "Chưa có cuộc trò chuyện nào")
                } else {
                    ChatListSuccess(
                        threads = state.threads,
                        onThreadClick = { onThreadClick(it.otherUserId) }
                    )
                }
            }
            is ChatListUiState.Error -> {
                val threads = (state as? ChatListUiState.Success)?.threads
                if (threads.isNullOrEmpty()) {
                    ErrorState(message = "Không thể tải danh sách cuộc trò chuyện")
                }
            }
        }
    }
}

@Composable
private fun ChatListSuccess(
    threads: List<ChatThreadUi>,
    onThreadClick: (ChatThreadUi) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(threads, key = { it.otherUserId }) { item ->
            ThreadRow(item = item, onClick = { onThreadClick(item) })
            Divider()
        }
    }
}

@Composable
private fun ListLoadingState() {
    LazyColumn {
        items(6) { ThreadSkeletonRow() }
    }
}

@Composable
private fun BoxScope.EmptyState(message: String) {
    Text(message, modifier = Modifier.align(Alignment.Center))
}

@Composable
private fun BoxScope.ErrorState(message: String) {
    Text(message, modifier = Modifier.align(Alignment.Center))
}

@Composable
private fun ThreadRow(item: ChatThreadUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarSize = 48.dp
        if (item.avatarUrl.isNullOrBlank()) {
            Surface(
                modifier = Modifier.size(avatarSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            AsyncImage(
                model = item.avatarUrl,
                contentDescription = "avatar",
                modifier = Modifier.size(avatarSize).clip(CircleShape)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatShortTime(item.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.lastMessage ?: "…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ThreadSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {}
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(0.6f).height(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}
        }
    }
}

private fun formatShortTime(ts: Long): String {
    if (ts == 0L) return ""
    val cal = Calendar.getInstance()
    val now = cal.timeInMillis
    val dayMs = 24 * 60 * 60 * 1000L
    return if (now - ts < dayMs) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
    } else {
        SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(ts))
    }
}