package com.bxt.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import coil.compose.rememberAsyncImagePainter
import com.bxt.ui.components.CustomSnackbar
import com.bxt.ui.components.CustomSnackbarHost
import com.bxt.ui.state.RegisterState
import com.bxt.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    onSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onAvatarChanged(uri)
    }

    val context = LocalContext.current

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
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Đăng ký tài khoản",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))


        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable(enabled = !viewModel.isLoading) {
                    imagePickerLauncher.launch("image/*")
                },
            contentAlignment = Alignment.Center
        ) {
            val currentAvatar = viewModel.avatarUri
            if (currentAvatar != null) {
                Image(
                    painter = rememberAsyncImagePainter(currentAvatar),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nút Thay đổi / Xoá ảnh (trước khi submit)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = !viewModel.isLoading
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Thay đổi avatar")
            }

            if (viewModel.avatarUri != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { viewModel.onAvatarChanged(null) },
                    enabled = !viewModel.isLoading
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Xoá ảnh")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Username
        OutlinedTextField(
            value = viewModel.username,
            onValueChange = viewModel::onUsernameChanged,
            label = { Text("Tên đăng nhập") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = viewModel::onEmailChanged,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Full name
        OutlinedTextField(
            value = viewModel.fullName,
            onValueChange = viewModel::onFullNameChanged,
            label = { Text("Họ và tên") },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            singleLine = true,
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = viewModel.phoneNumber,
            onValueChange = viewModel::onPhoneNumberChanged,
            label = { Text("Số điện thoại") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = viewModel::onPasswordChanged,
            label = { Text("Mật khẩu") },
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        viewModel.errorMessage?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.onRegisterClick(context) },
            enabled = !viewModel.isLoading && viewModel.avatarUri != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (viewModel.isLoading || uiState is RegisterState.Loading) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Text("Đăng ký")
            }
        }
    }
}
