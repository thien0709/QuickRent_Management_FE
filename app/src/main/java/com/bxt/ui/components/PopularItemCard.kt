package com.bxt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bxt.data.api.dto.response.ItemResponse
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// Hàm format tiền tệ (bạn có thể đặt ở file riêng)
fun formatVnd(amount: BigDecimal?): String {
    if (amount == null) return "N/A"
    val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
    return "${formatter.format(amount)} đ"
}

@Composable
fun PopularItemCard(
    item: ItemResponse,
    onClick: () -> Unit
) {
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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = item.imagePrimary,
                contentDescription = item.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title ?: "No Title",
                    fontSize = 16.sp, // ✨ Font nhỏ hơn
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2 // Giới hạn 2 dòng cho tiêu đề
                )

//                Text(
//                    text = item. ?: "N/A", // <-- SỬA LỖI Ở ĐÂY
//                    fontSize = 12.sp, // ✨ Font nhỏ hơn
//                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                )


                Text(
                    text = "Thuê: ${formatVnd(item.rentalPricePerHour)} / giờ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Cọc: ${formatVnd(item.depositAmount)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}