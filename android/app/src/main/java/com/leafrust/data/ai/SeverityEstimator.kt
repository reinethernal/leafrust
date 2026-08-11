package com.leafrust.data.ai

import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object SeverityEstimator {
    data class Result(
        val damagePercentage: Float,
        val leafPixels: Int,
        val lesionPixels: Int,
    )

    data class ColorStats(
        val meanH: Float,
        val meanS: Float,
        val meanV: Float,
        val rustRatio: Float,
        val whiteRatio: Float,
        val darkRatio: Float,
    )

    fun estimate(bitmap: Bitmap): Result {
        val step = max(1, min(bitmap.width, bitmap.height) / 160)
        var leaf = 0
        var lesion = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val c = bitmap.getPixel(x, y)
                if (Color.alpha(c) < 128) {
                    x += step
                    continue
                }
                val hsv = FloatArray(3)
                Color.RGBToHSV(Color.red(c), Color.green(c), Color.blue(c), hsv)
                val h = hsv[0]
                val s = hsv[1]
                val v = hsv[2]
                val isLeaf = isLeaf(h, s, v)
                val isLesion = isLesion(h, s, v)
                if (isLeaf || isLesion) {
                    leaf++
                    if (isLesion && !isHealthyGreen(h, s, v)) lesion++
                }
                x += step
            }
            y += step
        }
        if (leaf < 50) return Result(8f, leaf, lesion)
        val raw = lesion.toFloat() / leaf.toFloat() * 100f
        val pct = (round(min(98f, max(0f, raw)) * 10f) / 10f)
        return Result(pct, leaf, lesion)
    }

    fun colorStats(bitmap: Bitmap): ColorStats {
        val step = max(1, min(bitmap.width, bitmap.height) / 120)
        var n = 0
        var sumH = 0f
        var sumS = 0f
        var sumV = 0f
        var rust = 0
        var white = 0
        var dark = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val c = bitmap.getPixel(x, y)
                if (Color.alpha(c) < 128) {
                    x += step
                    continue
                }
                val hsv = FloatArray(3)
                Color.RGBToHSV(Color.red(c), Color.green(c), Color.blue(c), hsv)
                val h = hsv[0]
                val s = hsv[1]
                val v = hsv[2]
                if (v > 0.92f && s < 0.12f) {
                    white++
                    x += step
                    continue
                }
                n++
                sumH += h
                sumS += s
                sumV += v
                if (((h in 0f..40f) || h >= 330f) && s > 0.25f && v < 0.7f) rust++
                if (v < 0.2f) dark++
                x += step
            }
            y += step
        }
        if (n == 0) return ColorStats(90f, 0.4f, 0.5f, 0.1f, 0.05f, 0.05f)
        return ColorStats(
            meanH = sumH / n,
            meanS = sumS / n,
            meanV = sumV / n,
            rustRatio = rust.toFloat() / n,
            whiteRatio = white.toFloat() / max(1, white + n),
            darkRatio = dark.toFloat() / n,
        )
    }

    private fun isLeaf(h: Float, s: Float, v: Float) =
        (h in 35f..170f && s >= 0.12f && v in 0.12f..0.95f) ||
            (h in 25f..35f && s >= 0.18f && v >= 0.2f)

    private fun isLesion(h: Float, s: Float, v: Float): Boolean {
        val rustBrown = ((h in 0f..40f) || h >= 330f) && s >= 0.2f && v in 0.12f..0.75f
        val dark = v < 0.22f && s < 0.45f
        val chlorosis = h in 40f..70f && s >= 0.25f && v >= 0.35f
        return rustBrown || dark || chlorosis
    }

    private fun isHealthyGreen(h: Float, s: Float, v: Float) =
        h in 70f..160f && s >= 0.25f && v in 0.25f..0.9f
}
