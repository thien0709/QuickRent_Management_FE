package com.bxt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
    fromAddress: String?,
    toAddress: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = LocalDimens.current
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(d.pagePadding),
            verticalArrangement = Arrangement.spacedBy(d.rowGap)
        ) {
            Text(
                text = "Chuyến đi #${service.id ?: "—"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            RouteTimeline(
                from = fromAddress ?: "Đang lấy địa chỉ...",
                to   = toAddress   ?: "Đang lấy địa chỉ..."
            )

            InfoChips(
                departTime = service.departTime,
                availableSeat = service.availableSeat
            )

            if (!service.description.isNullOrBlank()) {
                Text(
                    text = service.description!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Giá vé",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(service.deliveryFee, nf),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private val RailWidth   = 18.dp
private val DotSize     = 8.dp
private val LineWidth   = 2.dp

@Composable
private fun RouteTimeline(
    from: String,
    to: String
) {
    val d = LocalDimens.current
    val lineColor = MaterialTheme.colorScheme.outlineVariant

     Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
         Box(
            modifier = Modifier
                .width(RailWidth)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(LineWidth)
                    .fillMaxHeight()
                    .padding(vertical = DotSize / 2)
                    .clip(RoundedCornerShape(LineWidth))
                    .background(lineColor)
            )

             Dot(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            Dot(
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Spacer(Modifier.width(d.rowGap))

         Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(d.rowGap)
        ) {
            AddressBlock(label = "Đi", text = from)
            AddressBlock(label = "Đến", text = to)
        }
    }
}

@Composable
private fun AddressBlock(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3
        )
    }
}

@Composable
private fun Dot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(DotSize)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun InfoChips(
    departTime: Instant?,
    availableSeat: Long?
) {
    val chipColors = AssistChipDefaults.assistChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = {},
            label = { Text(formatInstant(departTime)) },
            leadingIcon = { Icon(Icons.Filled.Event, null) },
            colors = chipColors,
            shape = MaterialTheme.shapes.medium
        )
        if (availableSeat != null) {
            AssistChip(
                onClick = {},
                label = { Text("Ghế trống: $availableSeat") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                colors = chipColors,
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, HH:mm • dd/MM/yyyy", Locale("vi", "VN"))
        .withZone(ZoneId.systemDefault())

private fun formatInstant(instant: Instant?): String =
    instant?.let { timeFormatter.format(it) } ?: "N/A"

private fun formatCurrency(amount: BigDecimal?, nf: NumberFormat): String =
    amount?.let { nf.format(it) } ?: "N/A"
