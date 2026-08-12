package com.leafrust.data.ai

import android.content.Context
import org.json.JSONObject

data class ModelSpec(
    val id: String,
    val titleRu: String,
    val subtitleRu: String,
    val assetModel: String?,
    val assetLabels: String?,
    val storageModel: String,
    val storageLabels: String,
    val versionFile: String,
    val shaFile: String,
    val minBytes: Long,
    val manifestUrls: List<String>,
    val modelUrls: List<String>,
    val labelUrls: List<String>,
)

object ModelCatalog {
    private const val ASSET_CATALOG = "models/models_catalog.json"

    fun load(context: Context): List<ModelSpec> {
        val text = try {
            context.assets.open(ASSET_CATALOG).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return listOf(fallbackV2())
        }
        return parse(text).ifEmpty { listOf(fallbackV2()) }
    }

    fun defaultId(context: Context): String {
        val text = try {
            context.assets.open(ASSET_CATALOG).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return fallbackV2().id
        }
        return try {
            JSONObject(text).optString("defaultModelId", fallbackV2().id)
        } catch (_: Exception) {
            fallbackV2().id
        }
    }

    fun byId(context: Context, id: String): ModelSpec {
        val all = load(context)
        return all.firstOrNull { it.id == id } ?: all.first()
    }

    private fun parse(text: String): List<ModelSpec> {
        val root = JSONObject(text)
        val arr = root.optJSONArray("models") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    ModelSpec(
                        id = o.getString("id"),
                        titleRu = o.optString("titleRu", o.getString("id")),
                        subtitleRu = o.optString("subtitleRu", ""),
                        assetModel = o.optString("assetModel", "").takeIf { it.isNotBlank() && it != "null" },
                        assetLabels = o.optString("assetLabels", "").takeIf { it.isNotBlank() && it != "null" },
                        storageModel = o.getString("storageModel"),
                        storageLabels = o.getString("storageLabels"),
                        versionFile = o.optString("versionFile", "${o.getString("id")}_version.txt"),
                        shaFile = o.optString("shaFile", "${o.getString("id")}_sha256.txt"),
                        minBytes = o.optLong("minBytes", 1_000_000L),
                        manifestUrls = o.optJSONArray("manifestUrls").toStringList(),
                        modelUrls = o.optJSONArray("modelUrls").toStringList(),
                        labelUrls = o.optJSONArray("labelUrls").toStringList(),
                    ),
                )
            }
        }
    }

    private fun org.json.JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }

    private fun fallbackV2() = ModelSpec(
        id = "plantvillage_v2",
        titleRu = "PlantVillage MobileNet",
        subtitleRu = "39 классов",
        assetModel = "models/plantvillage_mobilenet.tflite",
        assetLabels = "models/labels.txt",
        storageModel = "plantvillage_v2.tflite",
        storageLabels = "plantvillage_v2_labels.txt",
        versionFile = "plantvillage_v2_version.txt",
        shaFile = "plantvillage_v2_sha256.txt",
        minBytes = 1_000_000L,
        manifestUrls = ModelDownloader.MANIFEST_URLS,
        modelUrls = ModelDownloader.DEFAULT_MODEL_URLS,
        labelUrls = ModelDownloader.DEFAULT_LABEL_URLS,
    )
}
