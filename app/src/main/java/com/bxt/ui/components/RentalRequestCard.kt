package com.bxt.ui.components

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

@Composable
fun RentalRequestCard(
    data: RentalRequestResponse,
    isOwnerMode: Boolean,
    thumbnailUrl: String?,
    address: String?,
    onView: () -> Unit,
    onChangeStatus: (String) -> Unit
) {
    ElevatedCard {
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

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Yêu cầu #${data.id ?: "-"}", style = MaterialTheme.typography.titleMedium)
                    AssistChip(onClick = {}, label = { Text(data.status ?: "UNKNOWN") })
                }

                Text("Start: ${data.rentalStartTime ?: "-"}")
                Text("End: ${data.rentalEndTime ?: "-"}")
                Text("To: ${address ?: "Đang tải địa chỉ..."}")
                Text("Total: ${data.totalPrice ?: "-"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onView) { Text("Xem") }

                    var open by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { open = true }) { Text("Đổi trạng thái") }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            nextStatusesFor(data.status, isOwnerMode).forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st) },
                                    onClick = { open = false; onChangeStatus(st) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
// ... nextStatusesFor giữ nguyên

private fun nextStatusesFor(current: String?, isOwnerMode: Boolean): List<String> {
    val c = current?.uppercase().orEmpty()
    return when {
        c == "PENDING"  && isOwnerMode  -> listOf("APPROVED", "REJECTED")
        c == "APPROVED" && isOwnerMode  -> listOf("COMPLETED", "CANCELLED")
        c == "PENDING"  && !isOwnerMode -> listOf("CANCELLED")
        c == "APPROVED" && !isOwnerMode -> listOf("CANCELLED")
        else -> emptyList()
    }
}
