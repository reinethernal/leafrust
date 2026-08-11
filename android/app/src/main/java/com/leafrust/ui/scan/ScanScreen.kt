package com.leafrust.ui.scan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leafrust.LeafRustApp
import com.leafrust.util.AppLog
import com.leafrust.data.ai.ModelDownloadState
import com.leafrust.ui.components.GhostButton
import com.leafrust.ui.components.LeafBrand
import com.leafrust.ui.components.ModelDownloadBar
import com.leafrust.ui.components.PrimaryButton
import com.leafrust.ui.components.SecondaryButton
import com.leafrust.ui.theme.FieldBg
import com.leafrust.ui.theme.Ink
import com.leafrust.ui.theme.InkMuted
import com.leafrust.ui.theme.LeafGreen
import com.leafrust.ui.theme.LeafSoft
import kotlinx.coroutines.launch

@Composable
fun ScanScreen(onResult: (Long) -> Unit) {
    val app = LeafRustApp.instance
    val scope = rememberCoroutineScope()
    val downloadState by app.analyzer.downloadState.collectAsState()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var alignUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        app.analyzer.ensureModel()
    }

    fun analyze(uri: Uri) {
        scope.launch {
            busy = true
            error = null
            try {
                if (!app.analyzer.isModelReady()) {
                    app.analyzer.ensureModel()
                }
                AppLog.i("Scan", "analyze start")
                val result = app.analyzer.analyze(uri)
                AppLog.i("Scan", "analyze ok: " + result.plantSpecies + " / " + result.diseaseName)
                val id = app.repository.save(result)
                onResult(id)
            } catch (e: Exception) {
                AppLog.e("Scan", "analyze failed", e)
                error = e.message ?: "Ошибка анализа"
            } finally {
                busy = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { alignUri = it } }

    if (showCamera) {
        CameraWithGridScreen(
            onCaptured = { uri ->
                showCamera = false
                alignUri = uri
            },
            onClose = { showCamera = false },
        )
        return
    }

    alignUri?.let { uri ->
        AlignLeafScreen(
            sourceUri = uri,
            onAligned = { cropped ->
                alignUri = null
                analyze(cropped)
            },
            onClose = { alignUri = null },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(FieldBg, LeafSoft.copy(alpha = 0.4f), FieldBg))
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        LeafBrand(modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(
            text = "Сфотографируйте лист или выберите фото.\nОпределяются культуры, деревья, тропические и другие растения — с рекомендациями по лечению.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = InkMuted,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )

        when (val st = downloadState) {
            is ModelDownloadState.Checking,
            is ModelDownloadState.Downloading,
            is ModelDownloadState.Failed -> ModelDownloadBar(state = st)
            else -> Unit
        }

        if (busy) {
            Text("Анализируем лист…", style = MaterialTheme.typography.titleMedium, color = Ink)
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = LeafGreen,
                trackColor = LeafSoft,
            )
        }

        PrimaryButton(
            label = if (busy) "Подождите…" else "Сфотографировать",
            enabled = !busy,
            onClick = { showCamera = true },
        )
        SecondaryButton(
            label = "Выбрать из галереи",
            enabled = !busy,
            onClick = { galleryLauncher.launch("image/*") },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LeafSoft, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Как получить точный результат", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    "1. Поместите лист в рамку на экране подгонки.\n" +
                        "2. Можно увеличить, сдвинуть и повернуть фото.\n" +
                        "3. Снимайте при ровном свете, без сильных бликов.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
        }

        error?.let { msg ->
            Text(msg, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            GhostButton(
                label = "Скачать модель снова",
                onClick = { scope.launch { app.analyzer.downloadModel() } },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
