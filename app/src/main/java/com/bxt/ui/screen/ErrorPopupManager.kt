package com.bxt.ui.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest

data class ErrorEvent(
    val message: String,
    val canRetry: Boolean,
    val onRetry: (() -> Unit)? = null
)

object ErrorPopupManager {
    private val _errors = MutableSharedFlow<ErrorEvent>(
        extraBufferCapacity = 1
    )

    fun showError(message: String, canRetry: Boolean = false, onRetry: (() -> Unit)? = null) {
        _errors.tryEmit(ErrorEvent(message, canRetry, onRetry))
    }

    @Composable
    fun ErrorPopup() {
        var error by remember { mutableStateOf<ErrorEvent?>(null) }

        LaunchedEffect(Unit) {
            _errors.collectLatest { newError ->
                error = newError
            }
        }

        error?.let { currentError ->
            AlertDialog(
                onDismissRequest = { error = null },
                title = { Text("Lỗi") },
                text = { Text(currentError.message) },
                confirmButton = {
                    if (currentError.canRetry && currentError.onRetry != null) {
                        Button(onClick = {
                            currentError.onRetry.invoke()
                            error = null
                        }) {
                            Text("Thử lại")
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { error = null }) {
                        Text("Đóng")
                    }
                }
            )
        }
    }
}