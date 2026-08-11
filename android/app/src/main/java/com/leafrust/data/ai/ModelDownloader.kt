package com.leafrust.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

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
 * Bundled assets first; updates from the LeafRust GitHub Pages CDN
 * (`docs/models/model_manifest.json`). Falls back to PlantAi mirrors.
 */
class ModelDownloader(private val context: Context) {

    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    fun modelFile(): File = File(context.filesDir, "models/$MODEL_NAME")
    fun labelsFile(): File = File(context.filesDir, "models/$LABELS_NAME")
    private fun versionFile(): File = File(context.filesDir, "models/version.txt")
    private fun shaFile(): File = File(context.filesDir, "models/sha256.txt")

    fun isModelReady(): Boolean {
        val model = modelFile()
        return model.exists() && model.length() > MIN_MODEL_BYTES
    }

    fun localModelVersion(): Int =
        versionFile().takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 0

    /**
     * Uses the model bundled in the APK assets first (offline-ready).
     * Checks LeafRust CDN for a newer manifest version.
     * [forceDownload] always pulls from the network (Settings → Обновить модель).
     */
    suspend fun ensureModel(forceDownload: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        _state.value = ModelDownloadState.Checking
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()

        if (!forceDownload) {
            copyAssetIfPresent("models/$MODEL_NAME", modelFile())
            copyAssetIfPresent("models/$LABELS_NAME", labelsFile())
        }

        val remote = fetchManifest()
        val needUpdate = forceDownload ||
            !isModelReady() ||
            (remote != null && remote.version > localModelVersion()) ||
            (remote != null && remote.sha256.isNotBlank() && remote.sha256 != localSha())

        if (needUpdate && remote != null) {
            if (downloadFromManifest(remote)) {
                ensureLabels()
                _state.value = ModelDownloadState.Ready
                return@withContext true
            }
        }

        if (!forceDownload && isModelReady()) {
            ensureLabels()
            // Keep version at 0 until a CDN sync succeeds, so the first online
            // launch still pulls the published weights when they differ.
            if (!shaFile().exists() && modelFile().exists()) {
                shaFile().writeText(sha256(modelFile()))
            }
            _state.value = ModelDownloadState.Ready
            return@withContext true
        }

        val downloaded = downloadWithMirrors(modelFile(), MODEL_URLS)
        if (downloaded) {
            downloadWithMirrors(labelsFile(), LABELS_URLS) || writeDefaultLabels(labelsFile())
        }

        if (isModelReady()) {
            ensureLabels()
            versionFile().writeText((remote?.version ?: 1).toString())
            remote?.sha256?.takeIf { it.isNotBlank() }?.let { shaFile().writeText(it) }
            File(dir, ".downloaded").writeText(System.currentTimeMillis().toString())
            _state.value = ModelDownloadState.Ready
            return@withContext true
        }

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

    private fun localSha(): String {
        val stored = shaFile().takeIf { it.exists() }?.readText()?.trim().orEmpty()
        if (stored.isNotBlank()) return stored
        return if (modelFile().exists()) sha256(modelFile()) else ""
    }

    private data class Manifest(
        val version: Int,
        val sha256: String,
        val size: Long,
        val modelUrls: List<String>,
        val labelUrls: List<String>,
    )

    private fun fetchManifest(): Manifest? {
        for (url in MANIFEST_URLS) {
            try {
                val text = httpGetString(url) ?: continue
                val json = JSONObject(text)
                val version = json.optInt("version", 0)
                val sha = json.optString("sha256", "")
                val size = json.optLong("size", -1L)
                val urls = json.optJSONObject("urls")
                val modelUrls = urls?.optJSONArray("model").toStringList()
                    .ifEmpty { DEFAULT_MODEL_URLS }
                val labelUrls = urls?.optJSONArray("labels").toStringList()
                    .ifEmpty { DEFAULT_LABEL_URLS }
                return Manifest(
                    version = version,
                    sha256 = sha,
                    size = size,
                    modelUrls = (modelUrls + DEFAULT_MODEL_URLS).distinct(),
                    labelUrls = (labelUrls + DEFAULT_LABEL_URLS).distinct(),
                )
            } catch (_: Exception) {
                // try next
            }
        }
        return null
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }

    private fun downloadFromManifest(manifest: Manifest): Boolean {
        val okModel = downloadWithMirrors(
            dest = modelFile(),
            urls = manifest.modelUrls.ifEmpty { DEFAULT_MODEL_URLS },
            expectedSha = manifest.sha256,
            expectedSize = manifest.size,
        )
        if (!okModel) return false
        downloadWithMirrors(labelsFile(), manifest.labelUrls) || writeDefaultLabels(labelsFile())
        versionFile().writeText(manifest.version.toString())
        if (manifest.sha256.isNotBlank()) shaFile().writeText(manifest.sha256)
        File(context.filesDir, "models/.downloaded").writeText(System.currentTimeMillis().toString())
        return isModelReady()
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

    private fun downloadWithMirrors(
        dest: File,
        urls: List<String>,
        expectedSha: String = "",
        expectedSize: Long = -1L,
    ): Boolean {
        for (url in urls) {
            try {
                if (downloadFile(url, dest, expectedSize)) {
                    if (expectedSha.isNotBlank()) {
                        val got = sha256(dest)
                        if (!got.equals(expectedSha, ignoreCase = true)) {
                            dest.delete()
                            continue
                        }
                    }
                    return true
                }
            } catch (_: Exception) {
                // try next mirror
            }
        }
        return false
    }

    private fun downloadFile(urlString: String, dest: File, expectedSize: Long = -1L): Boolean {
        val fallbackTotal = when {
            expectedSize > 0 -> expectedSize
            dest.name.endsWith(".tflite") -> EXPECTED_MODEL_BYTES
            else -> -1L
        }
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

    private fun httpGetString(urlString: String): String? {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LeafRust/1.0")
        }
        return try {
            connection.connect()
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            var n: Int
            while (input.read(buf).also { n = it } != -1) {
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
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

        val MANIFEST_URLS = listOf(
            "https://reinethernal.github.io/leafrust/docs/models/model_manifest.json",
            "https://raw.githubusercontent.com/reinethernal/leafrust/main/docs/models/model_manifest.json",
        )

        val DEFAULT_MODEL_URLS = listOf(
            "https://reinethernal.github.io/leafrust/docs/models/plantvillage_mobilenet.tflite",
            "https://raw.githubusercontent.com/reinethernal/leafrust/main/docs/models/plantvillage_mobilenet.tflite",
        )

        val DEFAULT_LABEL_URLS = listOf(
            "https://reinethernal.github.io/leafrust/docs/models/labels.txt",
            "https://raw.githubusercontent.com/reinethernal/leafrust/main/docs/models/labels.txt",
        )

        val MODEL_URLS = DEFAULT_MODEL_URLS + listOf(
            "https://raw.githubusercontent.com/Nishant1998/PlantAi/master/model/model.tflite",
        )

        val LABELS_URLS = DEFAULT_LABEL_URLS
    }
}
