package com.bxt.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditLocationPopup(
    currentLocation: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var location by remember { mutableStateOf(currentLocation) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = "Chỉnh sửa địa chỉ")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Địa chỉ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(location) },
                enabled = location.isNotBlank()
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Hủy")
            }
        }
    )
}
