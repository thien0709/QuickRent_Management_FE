package com.bxt.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.viewmodel.ChatGeminiViewModel
import com.bxt.viewmodel.ChatGeminiUiState
import com.bxt.viewmodel.Message
import com.bxt.viewmodel.Participant
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.bxt.R
import androidx.compose.ui.text.input.ImeAction
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGeminiScreen(
    navController: NavController,
    viewModel: ChatGeminiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size, uiState.isBotTyping) {
        scope.launch {
            val extra = if (uiState.isBotTyping) 1 else 0
            val last = uiState.messages.lastIndex + extra
            if (last >= 0) listState.animateScrollToItem(last)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChatBot Assistant", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text = input,
                onTextChange = { input = it },
                onSendClicked = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        viewModel.sendMessage(text)
                        input = ""
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { message -> "${message.participant}-${message.text}-${System.nanoTime()}" }
                ) { message ->
                    MessageBubble(message)
                }
                if (uiState.isBotTyping) {
                    item { BotTypingBubbleLottie() }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.participant == Participant.USER
    val bg =
        if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg =
        if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isUser)
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    else
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            AvatarCircle(initial = "G")
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(min = 48.dp, max = 320.dp)
                .shadow(2.dp, shape)
                .clip(shape)
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.text,
                color = fg,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            AvatarCircle(initial = "U", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BotTypingBubbleLottie() {
    val shape =
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        AvatarCircle(initial = "G")
        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .widthIn(min = 72.dp, max = 220.dp)
                .shadow(2.dp, shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            LottieTypingIndicator()
        }
    }
}

@Composable
private fun LottieTypingIndicator() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.typing)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1.0f
    )

    if (composition == null) {
        Text(
            text = "Đang soạn…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    } else {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .height(24.dp)
                .width(64.dp)
        )
    }
}

@Composable
private fun AvatarCircle(initial: String, tint: Color = MaterialTheme.colorScheme.secondary) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
//@Composable
//private fun ChatInputBar(
//    modifier: Modifier = Modifier,
//    value: String,
//    enabled: Boolean,
//    onValueChange: (String) -> Unit,
//    onSend: () -> Unit
//) {
//    Surface(
//        modifier = modifier,
//        tonalElevation = 4.dp,
//        color = Color.Transparent
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .navigationBarsPadding()
//                .padding(horizontal = 8.dp, vertical = 8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//
//            val shape = RoundedCornerShape(24.dp)
//            var focused by remember { mutableStateOf(false) }
//            val borderWidth by animateDpAsState(if (focused) 2.dp else 1.dp, label = "borderW")
//            val borderColor by animateColorAsState(
//                if (focused) MaterialTheme.colorScheme.primary
//                else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
//                label = "borderC"
//            )
//
//            Box(
//                modifier = Modifier
//                    .weight(1f)
//                    .border(BorderStroke(borderWidth, borderColor), shape)
//                    .clip(shape)
//            ) {
//                OutlinedTextField(
//                    value = value,
//                    onValueChange = onValueChange,
//                    placeholder = { Text("Nhập tin nhắn…") },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .onFocusChanged { focused = it.isFocused }
//                        .padding(horizontal = 0.dp), // border đã ở ngoài
//                    shape = shape,
//                    maxLines = 4,
//                    enabled = enabled,
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedBorderColor = Color.Transparent,
//                        unfocusedBorderColor = Color.Transparent,
//                        disabledBorderColor = Color.Transparent,
//                        errorBorderColor = Color.Transparent,
//                        focusedContainerColor = Color.Transparent,
//                        unfocusedContainerColor = Color.Transparent,
//                        disabledContainerColor = Color.Transparent,
//                        errorContainerColor = Color.Transparent,
//                    ),
//                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
//                    keyboardActions = KeyboardActions(onSend = { onSend() })
//                )
//            }
//
//            Spacer(Modifier.width(8.dp))
//
//            FilledIconButton(
//                onClick = onSend,
//                enabled = enabled && value.isNotBlank()
//            ) {
//                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi")
//            }
//        }
//    }
//}

@Composable
private fun ChatInputBar(
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

