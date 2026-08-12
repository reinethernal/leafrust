package com.leafrust.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.leafrust.ui.theme.LeafGreen
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
    val selectedId by app.analyzer.selectedModelId.collectAsState(
        initial = app.analyzer.availableModels().firstOrNull()?.id.orEmpty(),
    )
    val models = remember { app.analyzer.availableModels() }
    val kbStats = remember { app.analyzer.knowledgeBase.stats() }
    var switching by remember { mutableStateOf(false) }

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
        Text(
            "Выберите веса для распознавания. Активная: ${app.analyzer.activeModelTitle()}",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
        )
        models.forEach { spec ->
            val selected = spec.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) LeafSoft else FieldBg, RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) LeafGreen else InkMuted.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable(enabled = !switching) {
                        if (spec.id == selectedId) return@clickable
                        switching = true
                        scope.launch {
                            try {
                                app.analyzer.selectModel(spec.id)
                            } finally {
                                switching = false
                            }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected,
                    onClick = {
                        if (spec.id == selectedId || switching) return@RadioButton
                        switching = true
                        scope.launch {
                            try {
                                app.analyzer.selectModel(spec.id)
                            } finally {
                                switching = false
                            }
                        }
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = LeafGreen),
                    enabled = !switching,
                )
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(spec.titleRu, style = MaterialTheme.typography.titleMedium, color = Ink)
                    if (spec.subtitleRu.isNotBlank()) {
                        Text(spec.subtitleRu, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                    }
                }
            }
        }

        when (downloadState) {
            is ModelDownloadState.Idle -> {
                Text(
                    if (app.analyzer.isModelReady()) {
                        if (switching) "Переключение модели…" else "Модель на устройстве готова"
                    } else {
                        "Модель ещё не скачана"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                )
            }
            else -> ModelDownloadBar(state = downloadState)
        }
        SecondaryButton(
            label = "Обновить выбранную модель",
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
