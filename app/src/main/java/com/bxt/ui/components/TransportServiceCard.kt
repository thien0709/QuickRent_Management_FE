package com.bxt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.bxt.ui.theme.LocalDimens
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
    val d = LocalDimens.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(d.pagePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.rowGap)
        ) {
            // (giữ nguyên không icon để không đổi layout)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Chuyến đi #${service.id ?: "—"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = service.description ?: "Chưa có mô tả",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(d.rowGap))

                Text(
                    text = "Khởi hành: ${formatInstant(service.departTime)}",
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = "Giá vé: ${formatCurrency(service.deliveryFee, currencyFormat)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            IconButton(
                onClick = { service.id?.let(onDelete) },
                enabled = service.id != null
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Xóa dịch vụ",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy").withZone(ZoneId.systemDefault())

private fun formatInstant(instant: Instant?): String =
    instant?.let { timeFormatter.format(it) } ?: "N/A"

private fun formatCurrency(amount: BigDecimal?, formatter: NumberFormat): String =
    amount?.let { formatter.format(it) } ?: "N/A"
