package com.leafrust.data.ai

/**
 * Symptom-based diagnosis for houseplants (PlantVillage TFLite has no indoor classes).
 */
object HouseplantDiagnoser {
    data class Result(val plant: PlantClass, val confidence: Float)

    fun diagnose(stats: SeverityEstimator.ColorStats, damage: Float): Result {
        val plant = when {
            damage < 6f && stats.rustRatio < 0.04f && stats.whiteRatio < 0.06f && stats.darkRatio < 0.08f ->
                PlantLabels.resolve("House___healthy")
            stats.whiteRatio > 0.16f -> PlantLabels.resolve("House___Powdery_mildew")
            stats.rustRatio > 0.12f && damage in 8f..35f -> PlantLabels.resolve("House___Spider_mites")
            stats.darkRatio > 0.18f && damage > 25f -> PlantLabels.resolve("House___Anthracnose")
            stats.darkRatio > 0.12f && stats.meanH in 15f..45f -> PlantLabels.resolve("House___Fungal_leaf_spot")
            stats.meanS < 0.28f && stats.meanV > 0.45f && damage > 10f -> PlantLabels.resolve("House___Chlorosis")
            stats.meanV > 0.72f && stats.meanS < 0.35f && damage > 12f -> PlantLabels.resolve("House___Sunburn")
            damage > 18f && stats.darkRatio < 0.1f && stats.meanH in 35f..75f -> PlantLabels.resolve("House___Overwatering")
            damage in 8f..22f && stats.darkRatio in 0.05f..0.14f -> PlantLabels.resolve("House___Bacterial_spot")
            damage > 10f && stats.meanV < 0.35f -> PlantLabels.resolve("House___Sooty_mold")
            damage in 5f..15f -> PlantLabels.resolve("House___Tip_burn")
            damage > 12f && stats.rustRatio < 0.08f -> PlantLabels.resolve("House___Edema")
            else -> PlantLabels.resolve("House___Fungal_leaf_spot")
        }
        val confidence = confidenceFor(plant, stats, damage)
        return Result(plant, confidence)
    }

    private fun confidenceFor(plant: PlantClass, stats: SeverityEstimator.ColorStats, damage: Float): Float {
        var score = 58f
        if (plant.healthy && damage < 6f) score += 22f
        if (!plant.healthy && damage >= 8f) score += 12f
        if (plant.id.contains("Powdery") && stats.whiteRatio > 0.16f) score += 14f
        if (plant.id.contains("Spider") && stats.rustRatio > 0.1f) score += 12f
        if (plant.id.contains("Chlorosis") && stats.meanS < 0.3f) score += 10f
        if (plant.id.contains("Overwatering") && damage > 15f) score += 8f
        return score.coerceIn(52f, 92f)
    }
}
