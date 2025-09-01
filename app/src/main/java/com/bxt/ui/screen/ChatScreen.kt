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

    // Tự động cuộn xuống tin nhắn mới nhất
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                            viewModel.sendMessage(text)
                            text = ""
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Màu nền tổng thể của màn hình chat
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            errorMessage?.let { err ->
                ErrorBanner(message = err)
            }

            if (!isUserLoggedIn) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Vui lòng đăng nhập để sử dụng tính năng này.")
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp), // Khoảng cách giữa các bong bóng
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp) // Padding cho toàn bộ danh sách
                ) {
                    items(messages, key = { it["timestamp"].toString() }) { message ->
                        MessageBubble(message, currentUserId)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    messageMap: Map<String, Any?>,
    myUserId: String?
) {
    val senderId = messageMap["senderId"] as? String
    val isMyMessage = myUserId != null && senderId == myUserId

    // Căn chỉnh bong bóng chat qua trái hoặc phải
    val horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        // Giới hạn chiều rộng của bong bóng chat để không bị tràn màn hình
        Box(modifier = Modifier.widthIn(min = 0.dp, max = 280.dp)) { // Giới hạn chiều rộng
            val textContent = (messageMap["text"] as? String)?.takeIf { it.isNotBlank() }
            val attachableContent = messageMap["attachable"] as? Map<String, Any?>

            // Định hình bo góc cho bong bóng chat
            val bubbleShape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMyMessage) 16.dp else 4.dp, // Góc gần người gửi ít bo hơn
                bottomEnd = if (isMyMessage) 4.dp else 16.dp    // Góc gần người gửi ít bo hơn
            )

            // Màu sắc bong bóng chat
            val bubbleColor = if (isMyMessage) {
                MaterialTheme.colorScheme.primary // Màu chủ đạo cho tin nhắn của mình
            } else {
                MaterialTheme.colorScheme.surface // Màu nền cho tin nhắn của người khác
            }

            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                tonalElevation = 2.dp
            ) {
                if (attachableContent != null) {
                    IntegratedMessageCard(
                        text = textContent,
                        attachableMap = attachableContent,
                        isMyMessage = isMyMessage
                    )
                } else if (textContent != null) {
                    Text(
                        text = textContent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
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
    val textColor = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val d = LocalDimens.current

    Column(modifier = Modifier.padding(8.dp)) {
        // Hiển thị nội dung thẻ sản phẩm (attachable)
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
                    } catch (e: Exception) { title ?: "Sản phẩm" }
                }

                Text(
                    decodedTitle,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium, // Điều chỉnh cỡ chữ
                    color = textColor
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Hiển thị tin nhắn đi kèm (nếu có)
        text?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = 4.dp),
                color = textColor
            )
        }
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") },
                shape = CircleShape, // Hình dạng TextField bo tròn hoàn toàn
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
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) // Màu khi bị disable
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = "Gửi")
            }
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