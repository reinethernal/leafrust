package com.leafrust.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leafrust.BuildConfig
import com.leafrust.LeafRustApp
import com.leafrust.data.ai.ModelDownloadState
import com.leafrust.data.share.ShareHelper
import com.leafrust.ui.components.GhostButton
import com.leafrust.ui.components.ModelDownloadBar
import com.leafrust.ui.components.PrimaryButton
import com.leafrust.ui.components.SecondaryButton
import com.leafrust.ui.theme.FieldBg
import com.leafrust.ui.theme.Ink
import com.leafrust.ui.theme.InkMuted
import com.leafrust.ui.theme.LeafSoft
import com.leafrust.ui.theme.RustSoft
import com.leafrust.util.AppLog
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val app = LeafRustApp.instance
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadState by app.analyzer.downloadState.collectAsState()
    val kbStats = remember { app.analyzer.knowledgeBase.stats() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium, color = Ink)

        Text("База растений", style = MaterialTheme.typography.titleLarge, color = Ink)
        Text(
            "Офлайн-справочник: ${kbStats.plants} растений, ${kbStats.diseases} типов болезней, "
                + "${kbStats.plantDiseases} связок растение–болезнь.",
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier
                .background(LeafSoft, RoundedCornerShape(14.dp))
                .padding(14.dp),
        )
        Text(
            "Симптомы и рекомендации подставляются из базы после распознавания. "
                + "Нейросеть PlantVillage покрывает агрокультуры; остальные виды — автоэвристика + справочник.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
        )

        Text("Модель ИИ", style = MaterialTheme.typography.titleLarge, color = Ink)
        when (downloadState) {
            is ModelDownloadState.Idle -> {
                Text(
                    if (app.analyzer.isModelReady()) "Модель на устройстве готова" else "Модель ещё не скачана",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                )
            }
            else -> ModelDownloadBar(state = downloadState)
        }
        SecondaryButton(
            label = "Обновить модель",
            onClick = { scope.launch { app.analyzer.downloadModel() } },
        )

        Text("Данные", style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.padding(top = 8.dp))
        GhostButton(
            label = "Экспортировать осмотры (CSV)",
            onClick = {
                scope.launch {
                    val items = app.repository.getAll()
                    ShareHelper.shareCsv(context, app.repository.toCsv(items))
                }
            },
        )

        if (BuildConfig.DEBUG) {
            Text("Отладка", style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Debug-сборка: логи анализа (KB hit/miss, модель, URI).",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
                modifier = Modifier
                    .background(RustSoft, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            )
            PrimaryButton(
                label = "Отправить логи",
                onClick = { AppLog.share(context) },
            )
            GhostButton(
                label = "Очистить логи",
                onClick = { AppLog.clear() },
            )
        }

        Text(
            text = "Отправка результата — через меню Android Share (WhatsApp, Telegram, почта и др.).",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            modifier = Modifier
                .background(LeafSoft, RoundedCornerShape(14.dp))
                .padding(14.dp),
        )
    }
}
