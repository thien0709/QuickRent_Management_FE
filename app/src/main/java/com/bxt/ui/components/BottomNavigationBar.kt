package com.bxt.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector


data class BottomNavItem(
    val label: String,                // Tên hiển thị
    val icon: ImageVector,            // Icon từ Material Icons
    val route: String                 // Route để xử lý điều hướng
)
@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    NavigationBar (
        containerColor = Color.White, // Bạn có thể tùy chỉnh màu sắc
    ) {
        items.forEach { item ->
            val isSelected = item.route == currentRoute

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item) },
                label = {
                    Text(text = item.label)
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6200EE), // Màu icon khi được chọn
                    unselectedIconColor = Color.Gray,      // Màu icon khi không được chọn
                    selectedTextColor = Color(0xFF6200EE),   // Màu chữ khi được chọn
                    unselectedTextColor = Color.Gray,        // Màu chữ khi không được chọn
                    indicatorColor = Color(0xFFE8DDFF)     // Màu nền của mục được chọn
                )
            )
        }
    }
}