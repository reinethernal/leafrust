package com.leafrust.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.leafrust.data.kb.KbEntry
import com.leafrust.data.kb.PlantKnowledgeBase
import com.leafrust.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class AnalysisResult(
    val plantSpecies: String,
    val diseaseName: String,
    val confidence: Float,
    val damagePercentage: Float,
    val symptoms: String,
    val treatment: String,
    val classId: String,
    val demoMode: Boolean,
    val healthy: Boolean,
    val imagePath: String,
    val plantKind: String,
)

class LeafAnalyzer(private val context: Context) {
    private val downloader = ModelDownloader(context)
    private val mutex = Mutex()
    private var classifier: TfliteClassifier? = null
    val knowledgeBase = PlantKnowledgeBase(context)

    val downloadState = downloader.state

    suspend fun ensureModel(): Boolean {
        AppLog.i("AI", "ensureModel start")
        val ok = downloader.ensureModel(forceDownload = false)
        if (ok) invalidateClassifier()
        AppLog.i(
            "AI",
            "ensureModel done ok=" + ok +
                " ready=" + isModelReady() +
                " bytes=" + downloader.modelFile().length(),
        )
        return ok
    }

    suspend fun downloadModel(): Boolean {
        AppLog.i("AI", "force download start")
        val ok = downloader.ensureModel(forceDownload = true)
        if (ok) invalidateClassifier()
        AppLog.i(
            "AI",
            "force download done ok=" + ok + " bytes=" + downloader.modelFile().length(),
        )
        return ok
    }

    fun isModelReady() = downloader.isModelReady()

    fun invalidateClassifier() {
        classifier?.close()
        classifier = null
        AppLog.i("AI", "classifier invalidated")
    }

    suspend fun analyze(uri: Uri): AnalysisResult = withContext(Dispatchers.IO) {
        AppLog.i("AI", "analyze uri=$uri")
        val jpeg = persistJpeg(uri)
        AppLog.i("AI", "persisted " + jpeg.absolutePath + " bytes=" + jpeg.length())

        val bitmap = BitmapFactory.decodeFile(jpeg.absolutePath)
            ?: error("Не удалось прочитать изображение")
        val argb = if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false).also {
                if (it !== bitmap) bitmap.recycle()
            }
        }
        AppLog.i("AI", "bitmap " + argb.width + "x" + argb.height)

        val severity = SeverityEstimator.estimate(argb)
        val stats = SeverityEstimator.colorStats(argb)
        AppLog.i(
            "AI",
            "damage=" + severity.damagePercentage +
                " leafPx=" + severity.leafPixels +
                " H=" + stats.meanH.toInt() +
                " rust=" + "%.2f".format(stats.rustRatio) +
                " white=" + "%.2f".format(stats.whiteRatio),
        )

        val tflite = mutex.withLock { tryLoadClassifier()?.predict(argb) }
        if (tflite != null) {
            AppLog.i(
                "AI",
                "tflite idx=" + tflite.index +
                    " id=" + tflite.classId +
                    " conf=" + "%.1f".format(tflite.confidence) +
                    " bg=" + tflite.isBackground +
                    " top2=" + tflite.top2.joinToString { it.first + ":" + "%.0f".format(it.second) },
            )
        } else {
            AppLog.w("AI", "tflite unavailable -> ornamental path")
        }

        val chosen = tflite?.let { pickUsefulPrediction(it) }

        if (chosen != null && !chosen.isBackground && chosen.confidence >= 35f) {
            val plant = enrichFromKb(PlantLabels.resolve(chosen.classId), chosen.classId)
            val damage = if (plant.healthy) {
                minOf(severity.damagePercentage, 5f)
            } else {
                severity.damagePercentage
            }
            AppLog.i("AI", "result TFLite -> " + plant.plantRu + " / " + plant.diseaseRu)
            return@withContext toResult(
                plant = plant,
                confidence = chosen.confidence.coerceIn(1f, 99.9f),
                damage = damage,
                imagePath = jpeg.absolutePath,
                demoMode = false,
            )
        }

        if (severity.leafPixels < 40 && (tflite?.isBackground == true || tflite == null)) {
            val bg = enrichFromKb(PlantLabels.resolve("Background"), "Background")
            AppLog.w("AI", "result Background (few leaf pixels)")
            return@withContext toResult(
                plant = bg,
                confidence = (tflite?.confidence ?: 40f).coerceIn(1f, 99.9f),
                damage = 0f,
                imagePath = jpeg.absolutePath,
                demoMode = tflite == null,
            )
        }

        val ornamental = OrnamentalDiagnoser.diagnose(stats, severity.damagePercentage)
        val plant = enrichFromKb(ornamental.plant, ornamental.plant.id)
        AppLog.i(
            "AI",
            "result ornamental -> " + plant.plantRu +
                " / " + plant.diseaseRu +
                " cat=" + PlantTaxonomy.categoryFor(plant.id),
        )
        toResult(
            plant = plant,
            confidence = ornamental.confidence,
            damage = if (plant.healthy) {
                minOf(severity.damagePercentage, 5f)
            } else {
                severity.damagePercentage
            },
            imagePath = jpeg.absolutePath,
            demoMode = tflite == null,
        )
    }

    /** Prefer offline KB texts when class key is known. */
    private fun enrichFromKb(plant: PlantClass, classKey: String): PlantClass {
        val entry = knowledgeBase.resolve(classKey)
            ?: knowledgeBase.resolve(plant.id)
            ?: return plant
        return plantFromKb(entry, fallbackId = plant.id, isBackground = plant.isBackground)
    }

    private fun plantFromKb(entry: KbEntry, fallbackId: String, isBackground: Boolean): PlantClass {
        return PlantClass(
            id = fallbackId,
            plantRu = entry.plantRu,
            diseaseRu = entry.diseaseRu,
            healthy = entry.healthy,
            symptoms = entry.symptomsRu,
            treatment = entry.treatmentRu,
            isBackground = isBackground || entry.plantId.contains("Background"),
        )
    }

    private fun pickUsefulPrediction(pred: TfliteClassifier.Prediction): TfliteClassifier.Prediction {
        if (!pred.isBackground) return pred
        val second = pred.top2.getOrNull(1) ?: return pred
        val (id, conf) = second
        if (id == "Background" || conf < 18f) return pred
        if (pred.confidence - conf > 25f) return pred
        AppLog.i("AI", "override Background -> " + id + " (" + "%.0f".format(conf) + "%)")
        return pred.copy(
            classId = id,
            confidence = conf,
            isBackground = false,
            index = PlantLabels.englishIds.indexOf(id).takeIf { it >= 0 } ?: pred.index,
        )
    }

    private fun toResult(
        plant: PlantClass,
        confidence: Float,
        damage: Float,
        imagePath: String,
        demoMode: Boolean,
    ): AnalysisResult {
        val kbCat = knowledgeBase.resolve(plant.id)?.category
        return AnalysisResult(
            plantSpecies = plant.plantRu,
            diseaseName = plant.diseaseRu,
            confidence = confidence,
            damagePercentage = damage,
            symptoms = plant.symptoms,
            treatment = plant.treatment,
            classId = plant.id,
            demoMode = demoMode,
            healthy = plant.healthy,
            imagePath = imagePath,
            plantKind = kbCat?.takeIf { it.isNotBlank() && it != "—" }
                ?: PlantTaxonomy.categoryFor(plant.id),
        )
    }

    private fun tryLoadClassifier(): TfliteClassifier? {
        if (classifier != null) return classifier
        val file = downloader.modelFile()
        AppLog.i(
            "AI",
            "load classifier exists=" + file.exists() +
                " bytes=" + file.length() +
                " path=" + file.absolutePath,
        )
        if (!file.exists() || file.length() < 100_000) {
            AppLog.w("AI", "model file missing/too small")
            return null
        }
        return try {
            TfliteClassifier(context, file).also {
                classifier = it
                AppLog.i("AI", "classifier ready " + it.debugInfo)
            }
        } catch (e: Exception) {
            AppLog.e("AI", "classifier load failed", e)
            null
        }
    }

    private fun persistJpeg(uri: Uri): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) dir.mkdirs()
        val dest = File(dir, "leaf_" + System.currentTimeMillis() + ".jpg")
        val original = decodeUri(uri) ?: error("Битый файл изображения / нет доступа к URI")
        val scaled = scale(original, 1280)
        FileOutputStream(dest).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        if (scaled !== original) scaled.recycle()
        original.recycle()
        return dest
    }

    private fun decodeUri(uri: Uri): Bitmap? {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                if (bmp != null) return bmp
            }
        } catch (e: Exception) {
            AppLog.w("AI", "openInputStream failed: " + e.message)
        }
        val path = uri.path
        if (!path.isNullOrBlank()) {
            val asFile = File(path)
            if (asFile.exists()) {
                val bmp = BitmapFactory.decodeFile(asFile.absolutePath)
                if (bmp != null) return bmp
            }
        }
        val name = path?.substringAfterLast('/')
        if (!name.isNullOrBlank()) {
            val cacheHit = File(context.cacheDir, name)
            if (cacheHit.exists()) {
                return BitmapFactory.decodeFile(cacheHit.absolutePath)
            }
        }
        AppLog.e("AI", "decodeUri failed for $uri")
        return null
    }

    private fun scale(src: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxSide) return src
        val ratio = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    fun close() {
        classifier?.close()
        classifier = null
        knowledgeBase.close()
    }
}
