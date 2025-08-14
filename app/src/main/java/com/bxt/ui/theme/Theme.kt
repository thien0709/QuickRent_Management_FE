package com.bxt.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF9800),    // Cam tươi
    onPrimary = Color.Black,
    secondary = Color(0xFFFFB74D),  // Cam nhạt
    onSecondary = Color.Black,
    tertiary = Color(0xFFFFCC80),   // Cam pastel
    background = Color(0xFF121212), // Đen
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),    // Đen xám
    onSurface = Color.White
)


private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFCC6600),    // Cam đất đậm
    onPrimary = Color.White,
    secondary = Color(0xFFFFD699),  // Cam pastel
    onSecondary = Color.Black,
    tertiary = Color(0xFFFFE8C2),   // Be vàng sáng
    background = Color(0xFFFCFAF5), // Be rất nhạt (gần trắng)
    onBackground = Color.Black,
    surface = Color(0xFFFFE4B5),    // Be đậm hơn cho nền BottomNav
    onSurface = Color.Black
)


@Composable
fun QuickRent_Management_FETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
