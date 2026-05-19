package com.zsilent.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zsilent.core.app.Screen
import com.zsilent.core.app.dashboardScreens
import com.zsilent.core.design.ZGreen
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.design.ZSurface
import com.zsilent.core.design.ZSurfaceHigh

@Composable
fun ZSidebar(
    selected: Screen,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(248.dp)
            .fillMaxHeight()
            .background(ZSurface)
            .padding(ZSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ZSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZSpacing.sm)) {
            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = ZGreen)
            Column {
                Text("ZSilent", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                Text("Core wallet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.padding(top = ZSpacing.sm))

        dashboardScreens.forEach { screen ->
            SidebarItem(
                screen = screen,
                selected = selected == screen,
                onClick = { onSelect(screen) }
            )
        }

        Spacer(Modifier.weight(1f))
        ZWarningBox(
            title = "Private by design",
            text = "Keys stay on this device."
        )
    }
}

@Composable
private fun SidebarItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) ZSurfaceHigh else Color.Transparent
    val color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = ZSpacing.md, vertical = ZSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(ZSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        screen.icon?.let { Icon(it, contentDescription = null, tint = color) }
        Text(screen.title, color = color, style = MaterialTheme.typography.titleMedium)
    }
}
