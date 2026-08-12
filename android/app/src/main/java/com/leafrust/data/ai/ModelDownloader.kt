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
 * Bundled assets first; updates from LeafRust GitHub Pages CDN per [ModelSpec].
 */
class ModelDownloader(private val context: Context) {

    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    private var active: ModelSpec = ModelCatalog.byId(context, ModelCatalog.defaultId(context))

    fun activeSpec(): ModelSpec = active

    fun setActiveSpec(spec: ModelSpec) {
        active = spec
    }

    fun modelFile(spec: ModelSpec = active): File = File(context.filesDir, "models/${spec.storageModel}")
    fun labelsFile(spec: ModelSpec = active): File = File(context.filesDir, "models/${spec.storageLabels}")
    private fun versionFile(spec: ModelSpec = active): File = File(context.filesDir, "models/${spec.versionFile}")
    private fun shaFile(spec: ModelSpec = active): File = File(context.filesDir, "models/${spec.shaFile}")

    fun isModelReady(spec: ModelSpec = active): Boolean {
        val model = modelFile(spec)
        return model.exists() && model.length() > spec.minBytes
    }

    fun localModelVersion(spec: ModelSpec = active): Int =
        versionFile(spec).takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 0

    /**
     * Uses the model bundled in the APK assets first (offline-ready).
     * Checks CDN for a newer manifest version.
     * [forceDownload] always pulls from the network (Settings → Обновить модель).
     */
    suspend fun ensureModel(
        spec: ModelSpec = active,
        forceDownload: Boolean = false,
    ): Boolean = withContext(Dispatchers.IO) {
        active = spec
        _state.value = ModelDownloadState.Checking
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()

        if (!forceDownload) {
            spec.assetModel?.let { copyAssetIfPresent(it, modelFile(spec)) }
            spec.assetLabels?.let { copyAssetIfPresent(it, labelsFile(spec)) }
            // Migrate old single-file layout for v2
            migrateLegacyIfNeeded(spec)
        }

        val remote = fetchManifest(spec)
        val needUpdate = forceDownload ||
            !isModelReady(spec) ||
            (remote != null && remote.version > localModelVersion(spec)) ||
            (remote != null && remote.sha256.isNotBlank() && remote.sha256 != localSha(spec))

        if (needUpdate && remote != null) {
            if (downloadFromManifest(spec, remote)) {
                ensureLabels(spec)
                _state.value = ModelDownloadState.Ready
                return@withContext true
            }
        }

        if (!forceDownload && isModelReady(spec)) {
            ensureLabels(spec)
            if (!shaFile(spec).exists() && modelFile(spec).exists()) {
                shaFile(spec).writeText(sha256(modelFile(spec)))
            }
            _state.value = ModelDownloadState.Ready
            return@withContext true
        }

        val urls = (spec.modelUrls + DEFAULT_MODEL_URLS).distinct()
        val downloaded = downloadWithMirrors(modelFile(spec), urls, minBytes = spec.minBytes)
        if (downloaded) {
            downloadWithMirrors(labelsFile(spec), spec.labelUrls.ifEmpty { DEFAULT_LABEL_URLS }, minBytes = 32) ||
                writeDefaultLabels(labelsFile(spec))
        }

        if (isModelReady(spec)) {
            ensureLabels(spec)
            versionFile(spec).writeText((remote?.version ?: 1).toString())
            remote?.sha256?.takeIf { it.isNotBlank() }?.let { shaFile(spec).writeText(it) }
            File(dir, ".downloaded_${spec.id}").writeText(System.currentTimeMillis().toString())
            _state.value = ModelDownloadState.Ready
            return@withContext true
        }

        spec.assetModel?.let { copyAssetIfPresent(it, modelFile(spec)) }
        spec.assetLabels?.let { copyAssetIfPresent(it, labelsFile(spec)) }
        if (isModelReady(spec)) {
            ensureLabels(spec)
            _state.value = ModelDownloadState.Ready
            return@withContext true
        }

        _state.value = ModelDownloadState.Failed("Не удалось получить модель «${spec.titleRu}».")
        false
    }

    private fun migrateLegacyIfNeeded(spec: ModelSpec) {
        if (spec.id != "plantvillage_v2") return
        val dest = modelFile(spec)
        if (dest.exists() && dest.length() > spec.minBytes) return
        val legacy = File(context.filesDir, "models/$MODEL_NAME")
        if (legacy.exists() && legacy.length() > spec.minBytes) {
            legacy.copyTo(dest, overwrite = true)
            val legacyLabels = File(context.filesDir, "models/$LABELS_NAME")
            if (legacyLabels.exists()) {
                legacyLabels.copyTo(labelsFile(spec), overwrite = true)
            }
        }
    }

    private fun localSha(spec: ModelSpec): String {
        val stored = shaFile(spec).takeIf { it.exists() }?.readText()?.trim().orEmpty()
        if (stored.isNotBlank()) return stored
        return if (modelFile(spec).exists()) sha256(modelFile(spec)) else ""
    }

    private data class Manifest(
        val version: Int,
        val sha256: String,
        val size: Long,
        val modelUrls: List<String>,
        val labelUrls: List<String>,
    )

    private fun fetchManifest(spec: ModelSpec): Manifest? {
        val urls = spec.manifestUrls.ifEmpty { MANIFEST_URLS }
        for (url in urls) {
            try {
                val text = httpGetString(url) ?: continue
                val json = JSONObject(text)
                val version = json.optInt("version", 0)
                val sha = json.optString("sha256", "")
                val size = json.optLong("size", -1L)
                val urlsObj = json.optJSONObject("urls")
                val modelUrls = urlsObj?.optJSONArray("model").toStringList()
                    .ifEmpty { spec.modelUrls }
                val labelUrls = urlsObj?.optJSONArray("labels").toStringList()
                    .ifEmpty { spec.labelUrls }
                return Manifest(
                    version = version,
                    sha256 = sha,
                    size = size,
                    modelUrls = (modelUrls + spec.modelUrls).distinct(),
                    labelUrls = (labelUrls + spec.labelUrls).distinct(),
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

    private fun downloadFromManifest(spec: ModelSpec, manifest: Manifest): Boolean {
        val okModel = downloadWithMirrors(
            dest = modelFile(spec),
            urls = manifest.modelUrls.ifEmpty { spec.modelUrls },
            expectedSha = manifest.sha256,
            expectedSize = manifest.size,
            minBytes = spec.minBytes,
        )
        if (!okModel) return false
        downloadWithMirrors(
            labelsFile(spec),
            manifest.labelUrls.ifEmpty { spec.labelUrls },
            minBytes = 32,
        ) || writeDefaultLabels(labelsFile(spec))
        versionFile(spec).writeText(manifest.version.toString())
        if (manifest.sha256.isNotBlank()) shaFile(spec).writeText(manifest.sha256)
        File(context.filesDir, "models/.downloaded_${spec.id}").writeText(System.currentTimeMillis().toString())
        return isModelReady(spec)
    }

    private fun ensureLabels(spec: ModelSpec) {
        if (!labelsFile(spec).exists() || labelsFile(spec).length() < 32) {
            spec.assetLabels?.let { copyAssetIfPresent(it, labelsFile(spec)) }
            if (!labelsFile(spec).exists() || labelsFile(spec).length() < 32) {
                writeDefaultLabels(labelsFile(spec))
            }
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
        minBytes: Long = MIN_MODEL_BYTES,
    ): Boolean {
        for (url in urls) {
            try {
                if (downloadFile(url, dest, expectedSize, minBytes)) {
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

    private fun downloadFile(
        urlString: String,
        dest: File,
        expectedSize: Long = -1L,
        minBytes: Long = MIN_MODEL_BYTES,
    ): Boolean {
        val fallbackTotal = expectedSize.takeIf { it > 0 } ?: -1L
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

        if (dest.name.endsWith(".tflite") && tmp.length() < minBytes) {
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
    }
}
