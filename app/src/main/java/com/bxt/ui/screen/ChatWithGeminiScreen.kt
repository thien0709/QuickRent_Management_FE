package com.bxt.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.viewmodel.ChatGeminiUiState
import com.bxt.viewmodel.ChatGeminiViewModel
import com.bxt.viewmodel.Message
import com.bxt.viewmodel.Participant
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGeminiScreen(viewModel: ChatGeminiViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var userPrompt by remember { mutableStateOf("") }

    // Tự động cuộn xuống tin nhắn mới nhất
    LaunchedEffect(uiState.messages.size) {
        coroutineScope.launch {
            if (uiState.messages.isNotEmpty()) {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemini Assistant") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message)
                }
                if (uiState.isBotTyping) {
                    item {
                        MessageBubble(Message("...", Participant.BOT, isPending = true))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = { Text("Nhập tin nhắn...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        viewModel.sendMessage(userPrompt)
                        userPrompt = ""
                    },
                    enabled = userPrompt.isNotBlank() && !uiState.isBotTyping
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Gửi")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val alignment = if (message.participant == Participant.USER) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.participant == Participant.USER) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (message.participant == Participant.USER) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Text(
            text = message.text,
            color = textColor,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(12.dp)
        )
    }
}