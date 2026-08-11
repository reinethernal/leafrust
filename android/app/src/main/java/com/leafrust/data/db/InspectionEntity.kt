package com.leafrust.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val imagePath: String,
    val plantSpecies: String,
    val diseaseName: String,
    val confidence: Float,
    val damagePercentage: Float,
    val symptoms: String,
    val treatment: String = "",
    val notes: String = "",
    val classId: String,
    val demoMode: Boolean,
    val plantKind: String = "",
)
