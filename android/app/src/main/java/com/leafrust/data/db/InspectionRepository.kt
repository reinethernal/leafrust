package com.leafrust.data.db

import android.content.Context
import com.leafrust.data.ai.AnalysisResult
import kotlinx.coroutines.flow.Flow

class InspectionRepository(context: Context) {
    private val dao = AppDatabase.get(context).inspectionDao()

    fun observeAll(): Flow<List<InspectionEntity>> = dao.observeAll()

    suspend fun getAll(): List<InspectionEntity> = dao.getAll()

    suspend fun get(id: Long): InspectionEntity? = dao.getById(id)

    suspend fun save(result: AnalysisResult, notes: String = ""): Long {
        return dao.insert(
            InspectionEntity(
                imagePath = result.imagePath,
                plantSpecies = result.plantSpecies,
                diseaseName = result.diseaseName,
                confidence = result.confidence,
                damagePercentage = result.damagePercentage,
                symptoms = result.symptoms,
                treatment = result.treatment,
                notes = notes,
                classId = result.classId,
                demoMode = result.demoMode,
                plantKind = result.plantKind,
            )
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)

    fun toCsv(items: List<InspectionEntity>): String {
        val header = "id,created_at,plant_species,disease_name,confidence,damage_percentage,symptoms,treatment,notes,class_id,demo_mode,plant_kind"
        val rows = items.joinToString("\n") { i ->
            listOf(
                i.id,
                i.createdAt,
                csv(i.plantSpecies),
                csv(i.diseaseName),
                i.confidence,
                i.damagePercentage,
                csv(i.symptoms),
                csv(i.treatment),
                csv(i.notes),
                csv(i.classId),
                i.demoMode,
                csv(i.plantKind),
            ).joinToString(",")
        }
        return "$header\n$rows"
    }

    private fun csv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }
}
