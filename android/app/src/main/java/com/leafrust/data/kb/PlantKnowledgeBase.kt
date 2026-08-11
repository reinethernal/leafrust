package com.leafrust.data.kb

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.leafrust.util.AppLog
import java.io.File
import java.io.FileOutputStream

data class KbEntry(
    val plantId: String,
    val plantRu: String,
    val plantEn: String,
    val category: String,
    val diseaseId: String,
    val diseaseRu: String,
    val diseaseEn: String,
    val diseaseKind: String,
    val symptomsRu: String,
    val treatmentRu: String,
    val healthy: Boolean,
    val classKey: String? = null,
)

data class KbStats(
    val plants: Int,
    val diseases: Int,
    val plantDiseases: Int,
    val classMap: Int,
)

/**
 * Offline plant/disease knowledge base (SQLite from assets).
 */
class PlantKnowledgeBase(context: Context) {
    private val appContext = context.applicationContext
    private val db: SQLiteDatabase

    init {
        db = openDatabase()
        val s = stats()
        AppLog.i(
            "KB",
            "ready plants=${s.plants} diseases=${s.diseases} pd=${s.plantDiseases} map=${s.classMap}",
        )
    }

    fun stats(): KbStats {
        fun count(table: String): Int =
            db.rawQuery("SELECT COUNT(*) FROM $table", null).use { c ->
                c.moveToFirst()
                c.getInt(0)
            }
        return KbStats(
            plants = count("plants"),
            diseases = count("diseases"),
            plantDiseases = count("plant_diseases"),
            classMap = count("class_map"),
        )
    }

    fun meta(key: String): String? =
        db.rawQuery("SELECT value FROM meta WHERE key=?", arrayOf(key)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    /** Resolve by TFLite / ornamental class key. */
    fun resolve(classKey: String): KbEntry? {
        val sql = """
            SELECT p.id, p.name_ru, p.name_en, p.category,
                   d.id, d.name_ru, d.name_en, d.kind,
                   pd.symptoms_ru, pd.treatment_ru, cm.class_key
            FROM class_map cm
            JOIN plant_diseases pd ON pd.id = cm.plant_disease_id
            JOIN plants p ON p.id = pd.plant_id
            JOIN diseases d ON d.id = pd.disease_id
            WHERE cm.class_key = ?
            LIMIT 1
        """.trimIndent()
        return db.rawQuery(sql, arrayOf(classKey)).use { c ->
            if (!c.moveToFirst()) {
                AppLog.i("KB", "miss key=$classKey")
                null
            } else {
                AppLog.i("KB", "hit key=$classKey -> ${c.getString(1)} / ${c.getString(5)}")
                rowToEntry(c)
            }
        }
    }

    /** Resolve by plant id + disease id. */
    fun resolvePlantDisease(plantId: String, diseaseId: String): KbEntry? {
        val sql = """
            SELECT p.id, p.name_ru, p.name_en, p.category,
                   d.id, d.name_ru, d.name_en, d.kind,
                   pd.symptoms_ru, pd.treatment_ru, NULL
            FROM plant_diseases pd
            JOIN plants p ON p.id = pd.plant_id
            JOIN diseases d ON d.id = pd.disease_id
            WHERE pd.plant_id = ? AND pd.disease_id = ?
            LIMIT 1
        """.trimIndent()
        return db.rawQuery(sql, arrayOf(plantId, diseaseId)).use { c ->
            if (!c.moveToFirst()) null else rowToEntry(c)
        }
    }

    fun search(query: String, limit: Int = 30): List<KbEntry> {
        val q = "%${query.trim()}%"
        if (q == "%%") return emptyList()
        val sql = """
            SELECT p.id, p.name_ru, p.name_en, p.category,
                   d.id, d.name_ru, d.name_en, d.kind,
                   pd.symptoms_ru, pd.treatment_ru, NULL
            FROM plant_diseases pd
            JOIN plants p ON p.id = pd.plant_id
            JOIN diseases d ON d.id = pd.disease_id
            WHERE p.name_ru LIKE ? OR p.name_en LIKE ? OR d.name_ru LIKE ? OR pd.aliases LIKE ?
            LIMIT ?
        """.trimIndent()
        return db.rawQuery(sql, arrayOf(q, q, q, q, limit.toString())).use { c ->
            buildList {
                while (c.moveToNext()) add(rowToEntry(c))
            }
        }
    }

    fun byCategory(category: String, limit: Int = 50): List<KbEntry> {
        val sql = """
            SELECT p.id, p.name_ru, p.name_en, p.category,
                   d.id, d.name_ru, d.name_en, d.kind,
                   pd.symptoms_ru, pd.treatment_ru, NULL
            FROM plant_diseases pd
            JOIN plants p ON p.id = pd.plant_id
            JOIN diseases d ON d.id = pd.disease_id
            WHERE p.category = ?
            LIMIT ?
        """.trimIndent()
        return db.rawQuery(sql, arrayOf(category, limit.toString())).use { c ->
            buildList {
                while (c.moveToNext()) add(rowToEntry(c))
            }
        }
    }

    private fun rowToEntry(c: android.database.Cursor): KbEntry {
        val diseaseId = c.getString(4)
        return KbEntry(
            plantId = c.getString(0),
            plantRu = c.getString(1),
            plantEn = c.getString(2),
            category = c.getString(3),
            diseaseId = diseaseId,
            diseaseRu = c.getString(5),
            diseaseEn = c.getString(6),
            diseaseKind = c.getString(7),
            symptomsRu = c.getString(8),
            treatmentRu = c.getString(9),
            healthy = diseaseId == "healthy",
            classKey = if (c.isNull(10)) null else c.getString(10),
        )
    }

    private fun openDatabase(): SQLiteDatabase {
        val dest = File(appContext.filesDir, "kb/plants_diseases.sqlite")
        dest.parentFile?.mkdirs()
        val assetVersion = readAssetVersion()
        val localVersion = File(appContext.filesDir, "kb/version.txt")
        val needCopy = !dest.exists() ||
            !localVersion.exists() ||
            localVersion.readText().trim() != assetVersion
        if (needCopy) {
            appContext.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            localVersion.writeText(assetVersion)
            AppLog.i("KB", "copied asset KB v=$assetVersion bytes=${dest.length()}")
        }
        return SQLiteDatabase.openDatabase(
            dest.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
    }

    private fun readAssetVersion(): String {
        // Use file length + known meta as cache buster when asset replaced
        return try {
            appContext.assets.open(ASSET_PATH).use { it.available().toString() }
        } catch (_: Exception) {
            "0"
        }
    }

    fun close() {
        db.close()
    }

    companion object {
        const val ASSET_PATH = "kb/plants_diseases.sqlite"
    }
}
