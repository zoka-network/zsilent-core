package com.zsilent.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zsilent.core.design.ZBorder
import com.zsilent.core.design.ZMutedBlue
import com.zsilent.core.design.ZSpacing

@Composable
fun ZWarningBox(
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(BorderStroke(1.dp, ZBorder), RoundedCornerShape(16.dp))
            .padding(ZSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ZSpacing.xs)
    ) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ZMutedBlue)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
