package com.leafrust.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.leafrust.ui.theme.Ink
import com.leafrust.ui.theme.LeafGreen
import com.leafrust.ui.theme.PillShape
import com.leafrust.ui.theme.Rust
import com.leafrust.ui.theme.RustSoft

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = PillShape,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = Color.White),
    ) {
        Text(label)
    }
}

@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(containerColor = RustSoft, contentColor = Ink),
    ) {
        Text(label, color = Rust)
    }
}

@Composable
fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = PillShape,
    ) {
        Text(label)
    }
}
