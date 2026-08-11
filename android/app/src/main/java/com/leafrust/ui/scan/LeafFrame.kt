package com.leafrust.ui.scan

import androidx.compose.ui.geometry.Size

/** Shared leaf capture / align frame (matches camera grid overlay). */
data class LeafFrameRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

fun leafFrameIn(size: Size): LeafFrameRect {
    val frameW = size.width * 0.72f
    val frameH = size.height * 0.48f
    return LeafFrameRect(
        left = (size.width - frameW) / 2f,
        top = (size.height - frameH) / 2f,
        width = frameW,
        height = frameH,
    )
}
