package com.bxt.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CustomSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    val isError = snackbarData.visuals.message.contains("lỗi", ignoreCase = true) ||
            snackbarData.visuals.message.contains("error", ignoreCase = true)

    val containerColor = if (isError) {
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.errorContainer
        else Color(0xFFD32F2F)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Snackbar(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(8.dp)),
        containerColor = containerColor,
        contentColor = contentColor,
//        actionColor = if (isError) Color.White else MaterialTheme.colorScheme.secondary,
        action = {
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                TextButton (
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = contentColor
                    )
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isError) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = contentColor
                )
            }
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

@Composable
fun CustomSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data -> CustomSnackbar(data) }
    )
}