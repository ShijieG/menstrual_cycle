package com.example.menstrual.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** 字体层级（沿用 Material3 默认，仅做轻量定制）。 */
val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp),
    titleMedium = TextStyle(fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 13.sp)
)
