package com.bxt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// TODO: thay thế FontFamily mặc định bằng font của bạn nếu có:
// val Inter = FontFamily(Font(R.font.inter_regular), ...)
private val Default = FontFamily.SansSerif

val CompactTypography = Typography().run {
    copy(
        titleSmall = titleSmall.copy(fontFamily = Default, fontSize = 16.sp),
        bodySmall  = bodySmall.copy(fontFamily = Default, fontSize = 13.sp),
        labelSmall = labelSmall.copy(fontFamily = Default, fontSize = 12.sp),
    )
}
