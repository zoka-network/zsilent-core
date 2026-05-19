package com.zsilent.core.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSecondaryButton
import com.zsilent.core.ui.components.ZSeedWordChip
import com.zsilent.core.ui.components.ZSectionTitle
import com.zsilent.core.ui.components.ZWarningBox

@Composable
fun SeedRevealScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Recovery phrase", "Write these words down in order.")
        ZWarningBox(
            title = "Sensitive screen",
            text = "Never share this phrase. It can restore the wallet without your device password."
        )
        ZCard {
            val words = appState.activeSeedWords()
            words.chunked(3).forEachIndexed { chunkIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.sm)) {
                    row.forEachIndexed { index, word ->
                        ZSeedWordChip(
                            index = chunkIndex * 3 + index,
                            word = word,
                            modifier = androidx.compose.ui.Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Text("The wallet password you create next does not replace this phrase.")
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZSecondaryButton("Back", appState::previousOnboarding)
            ZButton("I saved it", appState::nextOnboarding)
        }
    }
}
