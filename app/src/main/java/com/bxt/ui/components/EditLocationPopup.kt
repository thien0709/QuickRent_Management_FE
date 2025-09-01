package com.bxt.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point

@Composable
fun EditLocationPopup(
    currentLocation: String,
    proximity: Point?,                         // gợi ý quanh vị trí hiện tại; có thể null
    onDismiss: () -> Unit,
    onSave: (point: Point?, fullAddress: String) -> Unit, // ⬅️ trả về Point? + địa chỉ
    onGetCurrentLocation: () -> Unit,         // nút lấy địa chỉ hiện tại (GPS)
    isGettingCurrent: Boolean = false         // hiển thị loading/disable khi đang lấy
) {
    var query by remember { mutableStateOf(currentLocation) }
    var selectedPoint by remember { mutableStateOf<Point?>(null) }
    var selectedAddress by remember { mutableStateOf("") }

    // Nếu parent cập nhật currentLocation (sau khi bấm "Lấy địa chỉ hiện tại")
    LaunchedEffect(currentLocation) {
        if (currentLocation.isNotBlank()) {
            query = currentLocation
            selectedPoint = null
            selectedAddress = currentLocation
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa địa chỉ") },
        text = {
            Column {
                // ⬇️ thanh search + gợi ý Mapbox
                MapboxSearchBar(
                    value = query,
                    onValueChange = { q ->
                        query = q
                        if (q != selectedAddress) {
                            selectedPoint = null  // người dùng đang sửa tay
                            selectedAddress = ""
                        }
                    },
                    proximity = proximity,
                    onPlacePicked = { p, addr ->
                        selectedPoint = p
                        selectedAddress = addr
                        query = addr
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (selectedPoint == null)
                        "Nhập địa chỉ hoặc chọn một gợi ý trong danh sách."
                    else
                        "Đã chọn: $selectedAddress",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onGetCurrentLocation,
                    enabled = !isGettingCurrent
                ) {
                    if (isGettingCurrent) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        "Lấy vị trí",
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        val addr = selectedAddress.ifBlank { query.trim() }
                        onSave(selectedPoint, addr)
                    },
                    enabled = query.isNotBlank()
                ) {
                    Text("Lưu", maxLines = 1)
                }
            }
        }
    )
}
