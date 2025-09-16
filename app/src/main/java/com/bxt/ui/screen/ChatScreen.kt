package com.bxt.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bxt.ui.theme.LocalDimens
import com.bxt.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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

    val reversedMessages = remember(messages) { messages.asReversed() }
    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex <= 1 &&
                    listState.firstVisibleItemScrollOffset < 10
        }
    }

    LaunchedEffect(Unit) {
        if (reversedMessages.isNotEmpty()) listState.scrollToItem(0)
    }

    LaunchedEffect(reversedMessages.size) {
        if (reversedMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    LaunchedEffect(imeBottom) {
        if (reversedMessages.isNotEmpty() && isAtBottom) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipientName ?: "Đang tải...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (isUserLoggedIn) {
                MessageInput(
                    text = text,
                    onTextChange = { text = it },
                    onSendClicked = {
                        if (text.isNotBlank()) {
                            scope.launch { listState.scrollToItem(0) }
                            viewModel.sendMessage(text)
                            text = ""
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            errorMessage?.let { err -> ErrorBanner(message = err) }

            if (!isUserLoggedIn) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Vui lòng đăng nhập để sử dụng tính năng này.")
                }
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    items(
                        items = reversedMessages,
                        key = { messageKey(it) }
                    ) { message ->
                        MessageBubble(message, currentUserId)
                    }
                }
            }
        }
    }
}

private fun messageKey(m: Map<String, Any?>): Any =
    m["id"] ?: m["messageId"] ?: m["timestamp"] ?: m.hashCode()

@Composable
private fun MessageBubble(
    messageMap: Map<String, Any?>,
    myUserId: String?
) {
    val senderId = messageMap["senderId"] as? String
    val isMyMessage = myUserId != null && senderId == myUserId

    val horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.widthIn(min = 0.dp, max = 280.dp)) {
            val textContent = (messageMap["text"] as? String)?.takeIf { it.isNotBlank() }

            // Cast an toàn: Map<*, *> -> Map<String, Any?>
            val attachableContent: Map<String, Any?>? =
                (messageMap["attachable"] as? Map<*, *>)?.let { raw ->
                    buildMap<String, Any?> {
                        raw.forEach { (k, v) -> k?.toString()?.let { put(it, v) } }
                    }
                }

            val bubbleShape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMyMessage) 16.dp else 4.dp,
                bottomEnd = if (isMyMessage) 4.dp else 16.dp
            )
            val bubbleColor =
                if (isMyMessage) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface

            Surface(shape = bubbleShape, color = bubbleColor, tonalElevation = 2.dp) {
                when {
                    attachableContent != null -> {
                        IntegratedMessageCard(
                            text = textContent,
                            attachableMap = attachableContent,
                            isMyMessage = isMyMessage
                        )
                    }

                    textContent != null -> {
                        Text(
                            text = textContent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (isMyMessage)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegratedMessageCard(
    text: String?,
    attachableMap: Map<String, Any?>,
    isMyMessage: Boolean
) {
    val textColor =
        if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val d = LocalDimens.current

    Column(modifier = Modifier.padding(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(d.rowGap)
        ) {
            (attachableMap["image"] as? String)?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Attachment",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(d.rowGap))
            }
            Column {
                val title = attachableMap["title"] as? String
                val subtitle = attachableMap["subtitle"] as? String
                val decodedTitle = remember(title) {
                    try {
                        URLDecoder.decode(title, StandardCharsets.UTF_8.name())
                    } catch (_: Exception) {
                        title ?: "Sản phẩm"
                    }
                }
                Text(
                    decodedTitle, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium, color = textColor
                )
                subtitle?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
        text?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, modifier = Modifier.padding(horizontal = 4.dp), color = textColor)
        }
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    Surface(tonalElevation = 4.dp, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") },
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                maxLines = 5
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSendClicked,
                enabled = text.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) { Icon(Icons.Default.Send, contentDescription = "Gửi") }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}
