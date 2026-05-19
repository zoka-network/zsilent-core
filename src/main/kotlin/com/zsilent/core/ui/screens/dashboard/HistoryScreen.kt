package com.zsilent.core.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZGreen
import com.zsilent.core.design.ZRed
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.wallet.ActivityStatus

@Composable
fun HistoryScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
        appState.activity.forEach { item ->
            ZCard {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    item.amount,
                    color = when (item.status) {
                        ActivityStatus.Confirmed -> ZGreen
                        ActivityStatus.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
                        ActivityStatus.Failed -> ZRed
                    }
                )
            }
        }
    }
}
