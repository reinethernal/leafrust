package com.leafrust.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.leafrust.LeafRustApp
import com.leafrust.data.db.InspectionEntity
import com.leafrust.data.share.ShareHelper
import com.leafrust.ui.components.DamageRing
import com.leafrust.ui.components.GhostButton
import com.leafrust.ui.components.PrimaryButton
import com.leafrust.ui.theme.FieldBg
import com.leafrust.ui.theme.FieldElevated
import com.leafrust.ui.theme.Ink
import com.leafrust.ui.theme.InkMuted
import com.leafrust.ui.theme.LeafGreen
import com.leafrust.ui.theme.LeafSoft
import com.leafrust.ui.theme.RustSoft
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(
    id: Long,
    onBackToScan: () -> Unit,
    onDeleted: () -> Unit,
) {
    val app = LeafRustApp.instance
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<InspectionEntity?>(null) }

    LaunchedEffect(id) {
        item = app.repository.get(id)
    }

    val current = item
    if (current == null) {
        Column(
            modifier = Modifier.fillMaxSize().background(FieldBg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Загрузка…", style = MaterialTheme.typography.bodyLarge, color = InkMuted)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(FieldBg)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            AsyncImage(
                model = File(current.imagePath),
                contentDescription = "Снимок листа",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(240.dp),
            )

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (current.demoMode) {
                    InfoBanner(
                        "Демо-режим: нейросеть ещё не загружена. Показана оценка по цвету листа.",
                        RustSoft,
                    )
                }

                if (current.plantKind.isNotBlank() && current.plantKind != "—") {
                    Text(
                        text = current.plantKind,
                        style = MaterialTheme.typography.labelLarge,
                        color = LeafGreen,
                        modifier = Modifier
                            .background(LeafSoft, RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                Text(
                    text = current.plantSpecies,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                )
                Text(
                    text = current.diseaseName,
                    style = MaterialTheme.typography.titleLarge,
                    color = InkMuted,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DamageRing(current.damagePercentage)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(FieldElevated, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Уверенность", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                        Text(
                            text = "${"%.0f".format(current.confidence)}%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = LeafGreen,
                        )
                    }
                }

                SectionCard(title = "Симптомы", body = current.symptoms)
                if (current.treatment.isNotBlank()) {
                    SectionCard(
                        title = "Что делать",
                        body = current.treatment,
                        tint = LeafSoft,
                    )
                }

                Text(
                    text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(current.createdAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FieldElevated)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryButton(
                "Поделиться результатом",
                onClick = { ShareHelper.shareInspection(context, current) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Новый снимок", onClick = onBackToScan, modifier = Modifier.weight(1f))
                GhostButton(
                    "Удалить",
                    onClick = {
                        scope.launch {
                            app.repository.delete(current.id)
                            onDeleted()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String, bg: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Ink,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
}

@Composable
private fun SectionCard(
    title: String,
    body: String,
    tint: androidx.compose.ui.graphics.Color = FieldElevated,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Ink)
    }
}
