package com.leafrust.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class ModelDownloadState {
    data object Idle : ModelDownloadState()
    data object Checking : ModelDownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = -1L,
    ) : ModelDownloadState()
    data object Ready : ModelDownloadState()
    data class Failed(val message: String) : ModelDownloadState()
}

/**
 * Automatically downloads the on-device TFLite weights + labels on first launch
 * (or when files are missing / incomplete). Falls back to secondary mirror.
 */
class ModelDownloader(private val context: Context) {

    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    fun modelFile(): File = File(context.filesDir, "models/$MODEL_NAME")
    fun labelsFile(): File = File(context.filesDir, "models/$LABELS_NAME")

    fun isModelReady(): Boolean {
        val model = modelFile()
        return model.exists() && model.length() > MIN_MODEL_BYTES
    }

    /**
     * Uses the model bundled in the APK assets first (offline-ready).
     * Downloads from network only if assets are missing or [forceDownload] is true.
     */
    suspend fun ensureModel(forceDownload: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        _state.value = ModelDownloadState.Checking
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()

        // 1) Prefer APK-bundled model (works offline)
        if (!forceDownload) {
            copyAssetIfPresent("models/$MODEL_NAME", modelFile())
            copyAssetIfPresent("models/$LABELS_NAME", labelsFile())
            if (isModelReady()) {
                ensureLabels()
                _state.value = ModelDownloadState.Ready
                return@withContext true
            }
        }

        // 2) Download from PlantAi / mirrors
        val downloaded = downloadWithMirrors(modelFile(), MODEL_URLS)
        if (downloaded) {
            downloadWithMirrors(labelsFile(), LABELS_URLS) || writeDefaultLabels(labelsFile())
        }

        if (isModelReady()) {
            ensureLabels()
            File(dir, ".downloaded").writeText(System.currentTimeMillis().toString())
            _state.value = ModelDownloadState.Ready
            return@withContext true
        }

        // 3) Last resort: assets again
        copyAssetIfPresent("models/$MODEL_NAME", modelFile())
        copyAssetIfPresent("models/$LABELS_NAME", labelsFile())
        if (isModelReady()) {
            ensureLabels()
            _state.value = ModelDownloadState.Ready
            return@withContext true
        }

        _state.value = ModelDownloadState.Failed("Не удалось получить модель. Будет демо-режим.")
        false
    }

    private fun ensureLabels() {
        if (!labelsFile().exists() || labelsFile().length() < 32) {
            writeDefaultLabels(labelsFile())
        }
    }

    private fun copyAssetIfPresent(assetPath: String, dest: File): Boolean {
        return try {
            context.assets.open(assetPath).use { input ->
                dest.parentFile?.mkdirs()
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            dest.exists() && dest.length() > 32
        } catch (_: Exception) {
            false
        }
    }

    private fun downloadWithMirrors(dest: File, urls: List<String>): Boolean {
        for (url in urls) {
            try {
                if (downloadFile(url, dest)) return true
            } catch (_: Exception) {
                // try next mirror
            }
        }
        return false
    }

    private fun downloadFile(urlString: String, dest: File): Boolean {
        // Expected size for PlantAi model when server omits Content-Length
        val fallbackTotal = if (dest.name.endsWith(".tflite")) EXPECTED_MODEL_BYTES else -1L
        _state.value = ModelDownloadState.Downloading(0f, 0L, fallbackTotal)
        val tmp = File(dest.absolutePath + ".part")
        if (tmp.exists()) tmp.delete()

        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LeafRust/1.0")
        }

        connection.connect()
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            return false
        }
        val total = connection.contentLengthLong.takeIf { it > 0 } ?: fallbackTotal
        connection.inputStream.use { input ->
            FileOutputStream(tmp).use { output ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                var downloaded = 0L
                var lastEmit = 0L
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (downloaded - lastEmit >= 64 * 1024 || (total > 0 && downloaded >= total)) {
                        lastEmit = downloaded
                        val progress = if (total > 0) {
                            (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 0.99f)
                        } else {
                            (0.15f + (downloaded % (5L * 1024 * 1024)) / (5f * 1024 * 1024) * 0.75f)
                                .coerceIn(0.05f, 0.9f)
                        }
                        _state.value = ModelDownloadState.Downloading(progress, downloaded, total)
                    }
                }
            }
        }
        connection.disconnect()

        if (tmp.length() < MIN_MODEL_BYTES && dest.name.endsWith(".tflite")) {
            tmp.delete()
            return false
        }
        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        _state.value = ModelDownloadState.Downloading(1f, dest.length(), dest.length())
        return dest.exists()
    }

    private fun writeDefaultLabels(file: File): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(PlantLabels.englishIds.joinToString("\n") + "\n")
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val MODEL_NAME = "plantvillage_mobilenet.tflite"
        const val LABELS_NAME = "labels.txt"
        private const val MIN_MODEL_BYTES = 1_000_000L
        private const val EXPECTED_MODEL_BYTES = 11_285_824L

        val MODEL_URLS = listOf(
            // PlantAi ResNet-18 PlantVillage TFLite (~11 MB)
            // https://github.com/Nishant1998/PlantAi
            "https://raw.githubusercontent.com/Nishant1998/PlantAi/master/model/model.tflite",
            // Fallback mirrors
            "https://raw.githubusercontent.com/MustafaBeratYavas/plant-disease-edge-ai-diagnosis-system/main/mobile/assets/models/best_model_quantized.tflite",
            "https://huggingface.co/Agro-Tech-Ai/dr-disease-mobilenet-v2/resolve/main/dr_disease_model.tflite",
        )

        val LABELS_URLS = emptyList<String>()
    }
}
