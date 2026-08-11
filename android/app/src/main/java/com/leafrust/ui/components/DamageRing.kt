package com.leafrust.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leafrust.ui.theme.Border
import com.leafrust.ui.theme.Rust

@Composable
fun DamageRing(percent: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "damage",
    )
    Box(modifier = modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Border,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                color = Rust,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = stroke,
            )
        }
        Text(
            text = "${percent.toInt()}%",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Rust,
        )
    }
}
