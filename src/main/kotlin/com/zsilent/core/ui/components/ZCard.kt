package com.zsilent.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zsilent.core.design.ZBorder
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.design.ZSurface

@Composable
fun ZCard(
    modifier: Modifier = Modifier,
    background: Color = ZSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        modifier = modifier.border(BorderStroke(1.dp, ZBorder), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(ZSpacing.lg),
            content = content
        )
    }
}

@Composable
fun ZSectionTitle(title: String, subtitle: String? = null) {
    androidx.compose.material3.Text(title, style = MaterialTheme.typography.headlineMedium)
    if (subtitle != null) {
        androidx.compose.material3.Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
