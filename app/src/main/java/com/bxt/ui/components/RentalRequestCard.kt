package com.bxt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    isUpdating: Boolean, // Thêm tham số này để nhận trạng thái loading
    onView: () -> Unit,
    onChangeStatus: (String) -> Unit
) {
    ElevatedCard {
        // Box được dùng để có thể đặt lớp phủ loading lên trên
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

                    // Sử dụng hàm format để ngày tháng dễ đọc hơn
                    Text("Bắt đầu: ${formatInstant(data.rentalStartTime)}", style = MaterialTheme.typography.bodySmall)
                    Text("Kết thúc: ${formatInstant(data.rentalEndTime)}", style = MaterialTheme.typography.bodySmall)
                    Text("Đến: ${address ?: "Đang tải..."}", style = MaterialTheme.typography.bodySmall)
                    Text("Tổng: ${data.totalPrice ?: "-"}đ", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onView,
                            // Vô hiệu hóa nút khi đang cập nhật
                            enabled = !isUpdating
                        ) {
                            Text("Xem")
                        }

                        var open by remember { mutableStateOf(false) }
                        val nextStatuses = remember(data.status, isOwnerMode) {
                            nextStatusesFor(data.status, isOwnerMode)
                        }

                        // Chỉ hiển thị nút "Đổi trạng thái" nếu có trạng thái tiếp theo để chọn
                        if (nextStatuses.isNotEmpty()) {
                            Box {
                                Button(
                                    onClick = { open = true },
                                    // Vô hiệu hóa nút khi đang cập nhật
                                    enabled = !isUpdating
                                ) {
                                    Text("Đổi trạng thái")
                                }
                                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                    nextStatuses.forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(st) },
                                            onClick = {
                                                open = false
                                                onChangeStatus(st)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Lớp phủ loading sẽ hiển thị khi isUpdating là true
            if (isUpdating) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clip(CardDefaults.shape), // Bo góc theo Card
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// Giữ nguyên hàm logic này, nhưng sửa lại các trạng thái cho đúng với backend
private fun nextStatusesFor(current: String?, isOwnerMode: Boolean): List<String> {
    val c = current?.uppercase().orEmpty()
    return when {
        c == "PENDING" && isOwnerMode -> listOf("CONFIRMED", "REJECTED")
        c == "CONFIRMED" && isOwnerMode -> listOf("COMPLETED")
        c == "PENDING" && !isOwnerMode -> listOf("CANCELLED")
        c == "CONFIRMED" && !isOwnerMode -> listOf("CANCELLED")
        else -> emptyList()
    }
}

// Hàm tiện ích để format Instant thành chuỗi dd/MM/yyyy HH:mm cho dễ đọc
private fun formatInstant(instant: Instant?): String {
    if (instant == null) return "-"
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withLocale(Locale("vi", "VN"))
            .withZone(ZoneId.systemDefault()) // Dùng múi giờ của thiết bị
        formatter.format(instant)
    } catch (e: Exception) {
        "-"
    }
}