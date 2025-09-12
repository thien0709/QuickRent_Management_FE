package com.bxt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bxt.data.api.dto.response.RentalRequestResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RentalRequestCard(
    data: RentalRequestResponse,
    isOwnerMode: Boolean,
    thumbnailUrl: String?,
    address: String?,
    isUpdating: Boolean,
    onView: () -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit
) {
    val status = data.status?.uppercase()

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onView),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = thumbnailUrl ?: "https://via.placeholder.com/100",
                    contentDescription = "Ảnh sản phẩm",
                    modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f)) {
                    Text("Yêu cầu #${data.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Trạng thái: $status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Ngày thuê: ${formatInstant(data.rentalStartTime)}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (isUpdating) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                if (isOwnerMode && status == "PENDING") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        OutlinedButton(onClick = onReject) { Text("Từ chối") }
                        Button(onClick = onConfirm) { Text("Xác nhận") }
                    }
                }
                if (status in listOf("PENDING", "CONFIRMED")) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Text("Hủy yêu cầu")
                        }
                    }
                }
            }
        }
    }
}

private fun formatInstant(instant: Instant?): String {
    if (instant == null) return "N/A"
    return try {
        DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy")
            .withLocale(Locale("vi", "VN"))
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } catch (e: Exception) { "Invalid date" }
}