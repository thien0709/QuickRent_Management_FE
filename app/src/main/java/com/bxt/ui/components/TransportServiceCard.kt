package com.bxt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bxt.data.api.dto.response.TransportServiceResponse
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransportServiceCard(
    service: TransportServiceResponse,
    onDelete: (Long) -> Unit,
    onClick: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Optional: Add an icon here, e.g., Icon(Icons.Default.LocalShipping, null)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // In a real app, you would resolve these lat/lng to addresses
                Text(
                    text = "Chuyến đi #${service.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    // Fallback to description if addresses are not available
                    text = service.description ?: "Chưa có mô tả",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Khởi hành: ${formatInstant(service.departTime)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Giá vé: ${formatCurrency(service.deliveryFee, currencyFormat)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            IconButton(onClick = { service.id?.let { onDelete(it) } }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Xóa dịch vụ",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatInstant(instant: Instant?): String {
    if (instant == null) return "N/A"
    val formatter = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

private fun formatCurrency(amount: BigDecimal?, formatter: NumberFormat): String {
    if (amount == null) return "N/A"
    return formatter.format(amount)
}
