package com.leafrust.data.ai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

/**
 * TFLite inference aligned with PlantAi Android app:
 * https://github.com/Nishant1998/PlantAi
 *
 * Input: NCHW float32 [1,3,224,224] with ImageNet mean/std.
 * Output: 39 logits (softmax in post-process). Class 4 = Background.
 */
class TfliteClassifier(context: Context, modelFile: java.io.File) : AutoCloseable {

    private val interpreter: Interpreter
    private val inputHeight: Int
    private val inputWidth: Int
    private val channels: Int
    private val nchw: Boolean
    private val outputSize: Int
    private val labels: List<String>

    init {
        val options = Interpreter.Options().apply { setNumThreads(4) }
        interpreter = Interpreter(loadModelFile(modelFile), options)
        val inShape = interpreter.getInputTensor(0).shape()
        // PlantAi: [1, 3, 224, 224] NCHW; also support NHWC [1,224,224,3]
        when {
            inShape.size == 4 && inShape[1] == 3 -> {
                nchw = true
                channels = inShape[1]
                inputHeight = inShape[2]
                inputWidth = inShape[3]
            }
            inShape.size == 4 && inShape[3] == 3 -> {
                nchw = false
                inputHeight = inShape[1]
                inputWidth = inShape[2]
                channels = inShape[3]
            }
            else -> {
                nchw = true
                channels = 3
                inputHeight = 224
                inputWidth = 224
            }
        }
        outputSize = interpreter.getOutputTensor(0).shape().last()

        val labelsFile = java.io.File(context.filesDir, "models/${ModelDownloader.LABELS_NAME}")
        labels = if (labelsFile.exists()) {
            labelsFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            PlantLabels.englishIds
        }
    }

    data class Prediction(
        val classId: String,
        val confidence: Float,
        val index: Int,
        val isBackground: Boolean,
        val top2: List<Pair<String, Float>> = emptyList(),
    )

    val debugInfo: String
        get() = "in=${if (nchw) "NCHW" else "NHWC"} ${inputWidth}x${inputHeight} out=$outputSize labels=${labels.size}"


    fun predict(bitmap: Bitmap): Prediction {
        val resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val input = if (nchw) preprocessNchw(resized) else preprocessNhwc(resized)
        val output = Array(1) { FloatArray(outputSize) }
        interpreter.run(input, output)

        val probs = softmax(output[0])
        var bestIdx = 0
        var best = probs[0]
        for (i in 1 until probs.size) {
            if (probs[i] > best) {
                best = probs[i]
                bestIdx = i
            }
        }
        fun idAt(i: Int): String = labels.getOrElse(i) {
            PlantLabels.englishIds.getOrElse(i) { "Tomato___healthy" }
        }
        val ranked = probs.mapIndexed { i, p -> i to p }.sortedByDescending { it.second }
        val top2 = ranked.take(2).map { (i, p) -> idAt(i) to p * 100f }
        val id = idAt(bestIdx)
        return Prediction(
            classId = id,
            confidence = best * 100f,
            index = bestIdx,
            isBackground = bestIdx == PlantLabels.BACKGROUND_INDEX || id == "Background",
            top2 = top2,
        )
    }

    /** PlantAi Result.preprocessBitmap — NCHW + ImageNet normalize */
    private fun preprocessNchw(resized: Bitmap): ByteBuffer {
        val h = inputHeight
        val w = inputWidth
        val px = IntArray(w * h)
        resized.getPixels(px, 0, w, 0, 0, w, h)
        val buf = ByteBuffer.allocateDirect(4 * channels * h * w).order(ByteOrder.nativeOrder())
        // R plane
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val c = px[row + x]
                val r = ((c shr 16) and 0xFF) / 255f
                buf.putFloat((r - MEAN[0]) / STD[0])
            }
        }
        // G plane
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val c = px[row + x]
                val g = ((c shr 8) and 0xFF) / 255f
                buf.putFloat((g - MEAN[1]) / STD[1])
            }
        }
        // B plane
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val c = px[row + x]
                val b = (c and 0xFF) / 255f
                buf.putFloat((b - MEAN[2]) / STD[2])
            }
        }
        buf.rewind()
        return buf
    }

    private fun preprocessNhwc(resized: Bitmap): ByteBuffer {
        val h = inputHeight
        val w = inputWidth
        val px = IntArray(w * h)
        resized.getPixels(px, 0, w, 0, 0, w, h)
        val buf = ByteBuffer.allocateDirect(4 * h * w * 3).order(ByteOrder.nativeOrder())
        for (i in px.indices) {
            val c = px[i]
            val r = ((c shr 16) and 0xFF) / 255f
            val g = ((c shr 8) and 0xFF) / 255f
            val b = (c and 0xFF) / 255f
            buf.putFloat((r - MEAN[0]) / STD[0])
            buf.putFloat((g - MEAN[1]) / STD[1])
            buf.putFloat((b - MEAN[2]) / STD[2])
        }
        buf.rewind()
        return buf
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = FloatArray(logits.size) { exp((logits[it] - max).toDouble()).toFloat() }
        val sum = exps.sum().coerceAtLeast(1e-6f)
        return FloatArray(exps.size) { exps[it] / sum }
    }

    private fun loadModelFile(file: java.io.File): MappedByteBuffer {
        FileInputStream(file).use { input ->
            val channel = input.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
        }
    }

    override fun close() {
        interpreter.close()
    }

    companion object {
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}
