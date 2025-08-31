// bxt/ui/components/RentalRequestCard.kt

package com.bxt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
    onChangeStatus: (String) -> Unit
) {
    ElevatedCard {
        Box {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Yêu cầu #${data.id ?: "-"}", style = MaterialTheme.typography.titleMedium)
                        AssistChip(onClick = {}, label = { Text(data.status ?: "UNKNOWN") })
                    }

                    Text("Bắt đầu: ${formatInstant(data.rentalStartTime)}", style = MaterialTheme.typography.bodySmall)
                    Text("Kết thúc: ${formatInstant(data.rentalEndTime)}", style = MaterialTheme.typography.bodySmall)
                    Text("Đến: ${address ?: "Đang tải..."}", style = MaterialTheme.typography.bodySmall)
                    Text("Tổng: ${data.totalPrice ?: "-"}đ", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onView,
                            enabled = !isUpdating
                        ) {
                            Text("Xem chi tiết")
                        }

                        val currentStatus = data.status?.uppercase()
                        if (isOwnerMode) {
                            when (currentStatus) {
                                "PENDING" -> {
                                    Button(onClick = { onChangeStatus("CONFIRMED") }, enabled = !isUpdating) { Text("Chấp nhận") }
                                    Button(onClick = { onChangeStatus("REJECTED") }, enabled = !isUpdating, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Từ chối") }
                                }
                                "CONFIRMED" -> {
                                    Button(onClick = { onView() }, enabled = !isUpdating) {
                                        Text("Chuẩn bị")
                                    }
                                }
                            }
                        } else {
                            when (currentStatus) {
                                "PENDING", "CONFIRMED" -> {
                                    Button(onClick = { onChangeStatus("CANCELLED") }, enabled = !isUpdating) {
                                        Text("Hủy yêu cầu")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isUpdating) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clip(CardDefaults.shape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private fun formatInstant(instant: Instant?): String {
    if (instant == null) return "-"
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withLocale(Locale("vi", "VN"))
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "-"
    }
}