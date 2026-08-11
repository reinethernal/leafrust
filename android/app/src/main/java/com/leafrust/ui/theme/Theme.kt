package com.leafrust.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LeafGreen = Color(0xFF245C32)
val LeafSoft = Color(0xFFD5E6D1)
val Rust = Color(0xFFA34E24)
val RustSoft = Color(0xFFF2DDD0)
val FieldBg = Color(0xFFF2EEE4)
val FieldElevated = Color(0xFFFFFDF9)
val Ink = Color(0xFF142018)
val InkMuted = Color(0xFF3E4F42)
val Border = Color(0xFFCFC8BA)
val Danger = Color(0xFF9B3228)

private val LeafColorScheme = lightColorScheme(
    primary = LeafGreen,
    onPrimary = Color.White,
    secondary = Rust,
    onSecondary = Color.White,
    background = FieldBg,
    onBackground = Ink,
    surface = FieldElevated,
    onSurface = Ink,
    surfaceVariant = LeafSoft,
    onSurfaceVariant = InkMuted,
    error = Danger,
    outline = Border,
)

private val LeafTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.4).sp,
        color = LeafGreen,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = Ink,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = Ink,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        color = Ink,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        color = Ink,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = InkMuted,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
)

val PillShape = RoundedCornerShape(16.dp)

@Composable
fun LeafRustTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeafColorScheme,
        typography = LeafTypography,
        content = content,
    )
}
