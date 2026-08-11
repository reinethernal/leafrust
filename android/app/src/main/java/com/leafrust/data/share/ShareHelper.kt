package com.leafrust.data.share

import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.leafrust.data.db.InspectionEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareHelper {
    fun formatText(item: InspectionEntity): String {
        val whenStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(item.createdAt))
        return buildString {
            appendLine("LeafRust — результат осмотра")
            appendLine("Дата: $whenStr")
            appendLine("Категория: ${item.plantKind.ifBlank { "—" }}")
            appendLine("Растение: ${item.plantSpecies}")
            appendLine("Диагноз: ${item.diseaseName}")
            appendLine("Уверенность: ${"%.1f".format(item.confidence)}%")
            appendLine("Поражение: ${"%.1f".format(item.damagePercentage)}%")
            appendLine("Симптомы: ${item.symptoms}")
            if (item.treatment.isNotBlank()) appendLine("Рекомендации: ${item.treatment}")
            if (item.notes.isNotBlank()) appendLine("Заметки: ${item.notes}")
            appendLine(if (item.demoMode) "Режим: демо" else "Режим: локальный ИИ")
        }
    }

    /** Системный Android Sharesheet — WhatsApp, Telegram, Gmail, Диск и т.д. */
    fun shareInspection(context: Context, item: InspectionEntity) {
        val text = formatText(item)
        val imageFile = File(item.imagePath)
        val builder = ShareCompat.IntentBuilder(context)
            .setSubject("LeafRust: ${item.diseaseName}")
            .setText(text)
            .setChooserTitle("Отправить результат LeafRust")

        if (imageFile.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile,
            )
            builder
                .setType("image/jpeg")
                .setStream(uri)
        } else {
            builder.setType("text/plain")
        }

        val intent = builder.createChooserIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    fun shareCsv(context: Context, csv: String) {
        val file = File(context.cacheDir, "leafrust-inspections.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = ShareCompat.IntentBuilder(context)
            .setType("text/csv")
            .setStream(uri)
            .setSubject("LeafRust — осмотры")
            .setChooserTitle("Экспорт CSV")
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}
