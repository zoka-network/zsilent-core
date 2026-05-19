package com.zsilent.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.zsilent.core.design.ZBorder
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.design.ZSurfaceHigh

@Composable
fun ZSeedWordChip(
    index: Int,
    word: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(BorderStroke(1.dp, ZBorder), RoundedCornerShape(14.dp))
            .padding(horizontal = ZSpacing.md, vertical = ZSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(ZSpacing.sm)
    ) {
        Text(
            "${index + 1}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
        Text(word, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
    }
}
