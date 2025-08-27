package com.bxt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bxt.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages = viewModel.messages
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val recipientName by viewModel.recipientName.collectAsStateWithLifecycle()

    var text by remember { mutableStateOf("") }

    LaunchedEffect(isUserLoggedIn) { if (isUserLoggedIn) viewModel.listenForMessages() }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.size - 1) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipientName ?: "Chat với: ${viewModel.otherUserId}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (isUserLoggedIn) {
                MessageInput(
                    text = text,
                    onTextChange = { text = it },
                    onSendClicked = {
                        viewModel.sendMessage(text)
                        text = ""
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            errorMessage?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            if (!isUserLoggedIn) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bạn cần đăng nhập để sử dụng tính năng chat")
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages, key = { it["timestamp"].toString() }) { message ->
                        MessageBubbleFromMap(message, currentUserId)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleFromMap(
    messageMap: Map<String, Any?>,
    myUserId: String?
) {
    val senderId = messageMap["senderId"] as? String
    val isMyMessage = myUserId != null && senderId == myUserId
    val align = if (isMyMessage) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        (messageMap["attachable"] as? Map<String, Any?>)?.let { attach ->
            AttachableCardFromMap(attach)
            Spacer(Modifier.height(4.dp))
        }

        (messageMap["text"] as? String)?.takeIf { it.isNotBlank() }?.let { text ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isMyMessage)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (isMyMessage)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun AttachableCardFromMap(attachableMap: Map<String, Any?>) {
    Card {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            (attachableMap["image"] as? String)?.let { url ->
                AsyncImage(model = url, contentDescription = "Attachment", modifier = Modifier.size(50.dp))
                Spacer(Modifier.width(8.dp))
            }
            Column {
                val title = attachableMap["title"] as? String
                val subtitle = attachableMap["subtitle"] as? String
                Text(title ?: "Không có tiêu đề", fontWeight = FontWeight.Bold)
                subtitle?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 3
            )
            IconButton(onClick = onSendClicked, enabled = text.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Gửi")
            }
        }
    }
}
