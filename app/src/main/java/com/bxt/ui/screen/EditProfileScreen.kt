// bxt/ui/screen/EditProfileScreen.kt
package com.bxt.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bxt.data.api.dto.request.UpdateProfileRequest
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.theme.LocalDimens
import com.bxt.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: UserViewModel = hiltViewModel()
) {
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(updateState) {
        if (updateState.isSuccess) {
            Toast.makeText(context, "Update successful!", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            viewModel.resetUpdateState()
        }
        updateState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.resetUpdateState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime),
            contentAlignment = Alignment.Center
        ) {
            when {
                userProfileState.isLoading -> {
                    LoadingIndicator()
                }

                userProfileState.error != null -> {
                    ErrorContent(
                        error = userProfileState.error!!,
                        onRetry = { viewModel.fetchUserProfile() }
                    )
                }

                userProfileState.user is ApiResult.Success -> {
                    val currentUser = (userProfileState.user as ApiResult.Success<UserResponse>).data
                    EditProfileContent(
                        currentUser = currentUser,
                        isUpdating = updateState.isLoading,
                        onSave = { request, avatarUri ->
                            viewModel.updateUserProfile(request, avatarUri)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = onRetry) {
            Text("Thử lại")
        }
    }
}

@Composable
private fun EditProfileContent(
    currentUser: UserResponse,
    isUpdating: Boolean,
    onSave: (UpdateProfileRequest, Uri?) -> Unit
) {
    val dimens = LocalDimens.current

    var username by remember(currentUser.username) {
        mutableStateOf(currentUser.username.orEmpty())
    }
    var fullName by remember(currentUser.fullName) {
        mutableStateOf(currentUser.fullName.orEmpty())
    }
    var email by remember(currentUser.email) {
        mutableStateOf(currentUser.email.orEmpty())
    }
    var phone by remember(currentUser.phoneNumber) {
        mutableStateOf(currentUser.phoneNumber.orEmpty())
    }
    var pickedAvatarUri by remember { mutableStateOf<Uri?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> pickedAvatarUri = uri }
    )

    val isFormValid = remember(username, fullName, email) {
        username.isNotBlank() &&
                fullName.isNotBlank() &&
                email.isNotBlank() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.pagePadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        AvatarSection(
            avatarUrl = currentUser.avatarUrl,
            pickedAvatarUri = pickedAvatarUri,
            isUpdating = isUpdating,
            onPickImage = { pickImageLauncher.launch("image/*") }
        )

        ProfileInputFields(
            username = username,
            fullName = fullName,
            email = email,
            phone = phone,
            isUpdating = isUpdating,
            onUsernameChange = { username = it },
            onFullNameChange = { fullName = it },
            onEmailChange = { email = it },
            onPhoneChange = { phone = it }
        )

        SaveButton(
            isUpdating = isUpdating,
            isFormValid = isFormValid,
            onClick = {
                val request = UpdateProfileRequest(
                    username = username.trim(),
                    email = email.trim(),
                    fullName = fullName.trim(),
                    phone = phone.trim()
                )
                onSave(request, pickedAvatarUri)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AvatarSection(
    avatarUrl: String?,
    pickedAvatarUri: Uri?,
    isUpdating: Boolean,
    onPickImage: () -> Unit
) {
    val imageUrlWithCacheBuster = remember(avatarUrl) {
        avatarUrl?.let { "$it?t=${System.currentTimeMillis()}" }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        AsyncImage(
            model = pickedAvatarUri ?: imageUrlWithCacheBuster,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable(enabled = !isUpdating, onClick = onPickImage),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap to change profile picture",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ProfileInputFields(
    username: String,
    fullName: String,
    email: String,
    phone: String,
    isUpdating: Boolean,
    onUsernameChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text("Username") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !isUpdating,
        isError = username.isBlank()
    )

    OutlinedTextField(
        value = fullName,
        onValueChange = onFullNameChange,
        label = { Text("Full Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !isUpdating,
        isError = fullName.isBlank()
    )

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !isUpdating,
        isError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    )

    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Phone") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !isUpdating
    )
}

@Composable
private fun SaveButton(
    isUpdating: Boolean,
    isFormValid: Boolean,
    onClick: () -> Unit
) {
    Button(
        enabled = !isUpdating && isFormValid,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isUpdating) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text("Saving...")
            }
        } else {
            Text("Apply changes")
        }
    }
}