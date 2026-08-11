package com.leafrust.ui.inspections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.leafrust.LeafRustApp
import com.leafrust.data.share.ShareHelper
import com.leafrust.ui.components.GhostButton
import com.leafrust.ui.theme.FieldBg
import com.leafrust.ui.theme.FieldElevated
import com.leafrust.ui.theme.Ink
import com.leafrust.ui.theme.InkMuted
import com.leafrust.ui.theme.LeafGreen
import com.leafrust.ui.theme.LeafSoft
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun InspectionsScreen(onOpen: (Long) -> Unit) {
    val app = LeafRustApp.instance
    val items by app.repository.observeAll().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(FieldBg)) {
        Text(
            text = "История осмотров",
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        if (items.isNotEmpty()) {
            GhostButton(
                label = "Экспорт CSV",
                onClick = {
                    scope.launch {
                        ShareHelper.shareCsv(context, app.repository.toCsv(items))
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Пока пусто", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(
                    "Сделайте снимок на вкладке «Скан» — результат появится здесь.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(FieldElevated)
                            .clickable { onOpen(item.id) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model = File(item.imagePath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(88.dp)
                                .height(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LeafSoft),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.plantSpecies, style = MaterialTheme.typography.titleMedium, color = Ink)
                            Text(item.diseaseName, style = MaterialTheme.typography.bodyLarge, color = InkMuted)
                            Text(
                                listOfNotNull(
                                    item.plantKind.takeIf { it.isNotBlank() && it != "—" },
                                    "${"%.0f".format(item.damagePercentage)}% поражения",
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LeafGreen,
                            )
                            Text(
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(item.createdAt)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
