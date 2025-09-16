package com.bxt.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.bxt.ui.state.RegisterState
import com.bxt.ui.theme.LocalDimens
import com.bxt.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    onSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val d = LocalDimens.current
    val context = LocalContext.current

    var passwordVisible by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onAvatarChanged(uri)
    }

    val uiState = viewModel.uiState
    LaunchedEffect(uiState) {
        if (uiState is RegisterState.Success) {
            onSuccess()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = d.pagePadding)
            .windowInsetsPadding(WindowInsets.ime)
            .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Đăng ký tài khoản",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(d.sectionGap + 8.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(d.imageSize * 1.1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !viewModel.isLoading) {
                    imagePickerLauncher.launch("image/*")
                },
            contentAlignment = Alignment.Center
        ) {
            val currentAvatar = viewModel.avatarUri
            if (currentAvatar != null) {
                AsyncImage(
                    model = currentAvatar,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(d.rowGap))

        // Nút Thay đổi / Xoá ảnh
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = !viewModel.isLoading,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Thay đổi avatar", style = MaterialTheme.typography.bodySmall)
            }

            if (viewModel.avatarUri != null) {
                Spacer(Modifier.width(d.rowGap))
                TextButton(
                    onClick = { viewModel.onAvatarChanged(null) },
                    enabled = !viewModel.isLoading
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Xoá ảnh", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(d.sectionGap))

        // Username
        OutlinedTextField(
            value = viewModel.username,
            onValueChange = viewModel::onUsernameChanged,
            label = { Text("Tên đăng nhập", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            enabled = !viewModel.isLoading,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = d.fieldMinHeight),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(d.rowGap))

        // Email
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = viewModel::onEmailChanged,
            label = { Text("Email", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !viewModel.isLoading,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = d.fieldMinHeight),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(d.rowGap))

        // Full name
        OutlinedTextField(
            value = viewModel.fullName,
            onValueChange = viewModel::onFullNameChanged,
            label = { Text("Họ và tên", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            singleLine = true,
            enabled = !viewModel.isLoading,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = d.fieldMinHeight),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(d.rowGap))

        // Phone
        OutlinedTextField(
            value = viewModel.phoneNumber,
            onValueChange = viewModel::onPhoneNumberChanged,
            label = { Text("Số điện thoại", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = !viewModel.isLoading,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = d.fieldMinHeight),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(d.rowGap))

        // Password
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = viewModel::onPasswordChanged,
            label = { Text("Mật khẩu", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !viewModel.isLoading,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = d.fieldMinHeight),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(d.sectionGap))

        viewModel.errorMessage?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(d.rowGap / 2))
        }

        Button(
            onClick = { viewModel.onRegisterClick(context) },
            enabled = !viewModel.isLoading && viewModel.avatarUri != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(d.buttonHeight),
            shape = MaterialTheme.shapes.medium
        ) {
            if (viewModel.isLoading || uiState is RegisterState.Loading) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(d.progressSmall))
            } else {
                Text("Đăng ký", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
