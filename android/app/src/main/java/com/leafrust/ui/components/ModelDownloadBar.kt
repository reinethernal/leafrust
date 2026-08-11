package com.leafrust.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leafrust.data.ai.ModelDownloadState
import com.leafrust.ui.theme.FieldElevated
import com.leafrust.ui.theme.Ink
import com.leafrust.ui.theme.InkMuted
import com.leafrust.ui.theme.LeafGreen
import com.leafrust.ui.theme.LeafSoft
import com.leafrust.ui.theme.RustSoft
import java.util.Locale
import kotlin.math.max

@Composable
fun ModelDownloadBar(
    state: ModelDownloadState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ModelDownloadState.Checking -> {
            DownloadCard(
                title = "Проверка модели…",
                subtitle = "Ищем локальную копию, иначе скачаем из сети",
                progress = null,
                percentLabel = null,
                modifier = modifier,
            )
        }
        is ModelDownloadState.Downloading -> {
            val animated by animateFloatAsState(
                targetValue = state.progress.coerceIn(0f, 1f),
                label = "dlProgress",
            )
            val percent = (animated * 100f).toInt().coerceIn(0, 100)
            DownloadCard(
                title = "Скачивание модели из сети",
                subtitle = formatBytes(state.downloadedBytes, state.totalBytes),
                progress = animated,
                percentLabel = "$percent%",
                modifier = modifier,
            )
        }
        is ModelDownloadState.Ready -> {
            DownloadCard(
                title = "Модель готова",
                subtitle = "Локальная копия доступна на устройстве",
                progress = 1f,
                percentLabel = "100%",
                ready = true,
                modifier = modifier,
            )
        }
        is ModelDownloadState.Failed -> {
            DownloadCard(
                title = "Не удалось загрузить модель",
                subtitle = state.message,
                progress = null,
                percentLabel = null,
                failed = true,
                modifier = modifier,
            )
        }
        else -> Unit
    }
}

@Composable
private fun DownloadCard(
    title: String,
    subtitle: String,
    progress: Float?,
    percentLabel: String?,
    modifier: Modifier = Modifier,
    ready: Boolean = false,
    failed: Boolean = false,
) {
    val bg = when {
        failed -> RustSoft
        ready -> LeafSoft
        else -> FieldElevated
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            if (percentLabel != null) {
                Text(
                    text = percentLabel,
                    fontWeight = FontWeight.ExtraBold,
                    color = LeafGreen,
                )
            }
        }
        Text(text = subtitle, color = InkMuted)
        if (progress == null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = LeafGreen,
                trackColor = LeafSoft,
            )
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = LeafGreen,
                trackColor = LeafSoft,
            )
        }
    }
}

private fun formatBytes(downloaded: Long, total: Long): String {
    fun mb(v: Long): String =
        String.format(Locale.getDefault(), "%.1f МБ", v / (1024.0 * 1024.0))
    return when {
        total > 0 -> mb(downloaded) + " из " + mb(max(total, downloaded))
        downloaded > 0 -> "Загружено " + mb(downloaded) + "…"
        else -> "Подключение к серверу…"
    }
}
