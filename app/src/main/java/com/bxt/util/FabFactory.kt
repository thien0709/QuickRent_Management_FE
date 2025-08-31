package com.bxt.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bxt.ui.components.FabAction

@Composable
fun NotificationIcon(count: Int = 0) {
    Box {
        Icon(Icons.Default.Notifications, null)
        if (count > 0) {
            Box(
                Modifier
                    .offset(6.dp, (-6).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                Alignment.Center
            ) {
                Text(
                    if (count > 9) "9+" else count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

object FabActions {
    fun profile(nav: NavController) = listOf(
        FabAction("Thông báo", { NotificationIcon(1) }) { nav.navigate("notifications") },
        FabAction("Vận chuyển", { Icon(Icons.Default.GridView, null) }) { nav.navigate("transport") },
        FabAction("Thuê xe", { Icon(Icons.Default.Edit, null) }) { nav.navigate("rental") }
    )

    fun rental(nav: NavController) = listOf(
        FabAction("Add item ", { Icon(Icons.Default.Add, null) }) { nav.navigate("add_item") },
        FabAction("See request", { Icon(Icons.Default.Assignment, null) }) { nav.navigate("rental_service") },
        FabAction("See transaction", { Icon(Icons.Default.Receipt, null) }) { nav.navigate("transactions_rental") },
    )

    fun transport(nav: NavController) = listOf(
        FabAction("Add item ", { Icon(Icons.Default.Add, null) }) { nav.navigate("add_transport_service") },
        FabAction("See request", { Icon(Icons.Default.Assignment, null) }) { nav.navigate("transport_service") },
        FabAction("See transaction", { Icon(Icons.Default.Receipt, null) }) { nav.navigate("transactions_transport") },
    )

    fun home(nav: NavController) = listOf(
        FabAction("Quét QR", { Icon(Icons.Default.QrCodeScanner, null) }) { nav.navigate("qr") },
        FabAction("Thông báo", { NotificationIcon(3) }) { nav.navigate("notifications") },
        FabAction("Hỗ trợ", { Icon(Icons.Default.Help, null) }) { nav.navigate("support") }
    )

}
