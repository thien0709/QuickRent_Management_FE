package com.bxt.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

data class Dimens(
    val fieldMinHeight: Dp = 44.dp,
    val buttonHeight: Dp = 44.dp,
    val smallButtonHeight: Dp = 40.dp,
    val pagePadding: Dp = 16.dp,
    val sectionGap: Dp = 12.dp,
    val rowGap: Dp = 10.dp,
    val imageSize: Dp = 96.dp,
    val imageCorner: Dp = 10.dp,
    val iconSmall: Dp = 22.dp,
    val progressSmall: Dp = 18.dp,
)

val LocalDimens = staticCompositionLocalOf { Dimens() }
