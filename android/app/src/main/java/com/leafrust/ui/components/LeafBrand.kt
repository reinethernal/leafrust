package com.leafrust.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leafrust.R
import com.leafrust.ui.theme.LeafGreen

@Composable
fun LeafBrand(
    modifier: Modifier = Modifier,
    iconSize: Dp = 72.dp,
    animate: Boolean = true,
) {
    val scale = if (animate) {
        val t = rememberInfiniteTransition(label = "leafPulse")
        val s by t.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
            label = "scale",
        )
        s
    } else {
        1f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.leaf_icon),
            contentDescription = null,
            modifier = Modifier.size(iconSize).scale(scale),
        )
        Text(
            text = "LeafRust",
            style = MaterialTheme.typography.displayLarge,
            color = LeafGreen,
        )
    }
}
