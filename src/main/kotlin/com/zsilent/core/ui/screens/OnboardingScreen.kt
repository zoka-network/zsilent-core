package com.zsilent.core.ui.screens

import androidx.compose.runtime.Composable
import com.zsilent.core.models.OnboardingStep
import com.zsilent.core.models.SeedWordCount
import com.zsilent.core.models.WalletSetupMode
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.onboarding.AccountStepNoir
import com.zsilent.core.ui.onboarding.CreatePasswordStepNoir
import com.zsilent.core.ui.onboarding.DashboardReadyStepNoir
import com.zsilent.core.ui.onboarding.OnboardingShell
import com.zsilent.core.ui.onboarding.PrivacyStepNoir
import com.zsilent.core.ui.onboarding.RecoveryRevealStepNoir
import com.zsilent.core.ui.onboarding.SelfCustodyStepNoir
import com.zsilent.core.ui.onboarding.TutorialStepNoir
import com.zsilent.core.ui.onboarding.VerifySeedStepNoir
import com.zsilent.core.ui.onboarding.WalletChoiceStepNoir
import com.zsilent.core.ui.onboarding.WelcomeStepNoir

@Composable
fun OnboardingScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    OnboardingShell(
        step = ui.onboardingStep,
        busy = ui.onboardingBusy,
        canContinue = ui.onboardingStep.canContinue(ui),
        primaryLabel = ui.onboardingStep.primaryActionLabel(ui),
        error = ui.onboardingError,
        canBack = ui.onboardingStep != OnboardingStep.Welcome &&
            !(ui.onboardingStep == OnboardingStep.Account && (ui.onboardingBusy || ui.onboardingWalletAddress.isNotBlank())) &&
            !ui.onboardingBusy,
        onBack = appState::onboardingBack,
        onNext = appState::onboardingNext
    ) {
        when (ui.onboardingStep) {
            OnboardingStep.Welcome -> WelcomeStepNoir()
            OnboardingStep.SelfCustody -> SelfCustodyStepNoir(ui, appState)
            OnboardingStep.Wallet -> WalletChoiceStepNoir(ui, appState)
            OnboardingStep.Recovery -> RecoveryRevealStepNoir(ui)
            OnboardingStep.VerifySeed -> VerifySeedStepNoir(ui, appState)
            OnboardingStep.Security -> CreatePasswordStepNoir(ui, appState)
            OnboardingStep.Account -> AccountStepNoir(ui)
            OnboardingStep.Privacy -> PrivacyStepNoir(ui, appState)
            OnboardingStep.Tutorial -> TutorialStepNoir(ui, appState)
            OnboardingStep.Dashboard -> DashboardReadyStepNoir(ui)
        }
    }
}

private fun OnboardingStep.canContinue(ui: ZSilentUiState): Boolean = when (this) {
    OnboardingStep.Welcome -> true
    OnboardingStep.SelfCustody -> ui.onboardingSelfCustodyConfirmed
    OnboardingStep.Wallet -> {
        val words = (ui.onboardingRecoveryPhrase ?: ui.onboardingSeedPhrase)
            .trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        ui.onboardingWalletName.isNotBlank() &&
            (ui.walletSetupMode == WalletSetupMode.CreateNew || words.size in setOf(12, 24))
    }
    OnboardingStep.Recovery -> (ui.onboardingRecoveryPhrase ?: ui.onboardingSeedPhrase).isNotBlank()
    OnboardingStep.VerifySeed -> ui.onboardingSeedCheckFirst.isNotBlank() &&
        ui.onboardingSeedCheckSecond.isNotBlank()
    OnboardingStep.Security -> ui.onboardingPassword.length >= 8 &&
        ui.onboardingPassword == ui.onboardingPasswordConfirm
    OnboardingStep.Account -> ui.onboardingWalletAddress.isNotBlank()
    OnboardingStep.Privacy -> ui.onboardingPrivacyConfirmed
    OnboardingStep.Tutorial -> ui.onboardingTutorialConfirmed
    OnboardingStep.Dashboard -> true
}

private fun OnboardingStep.primaryActionLabel(ui: ZSilentUiState): String = when {
    ui.onboardingBusy -> "Working..."
    this == OnboardingStep.Welcome -> "Get started"
    this == OnboardingStep.Wallet && ui.walletSetupMode == WalletSetupMode.CreateNew -> "Generate phrase"
    this == OnboardingStep.Wallet && ui.walletSetupMode == WalletSetupMode.ImportSeed -> "Restore wallet"
    this == OnboardingStep.Recovery -> "I wrote it down"
    this == OnboardingStep.VerifySeed -> "Confirm recovery phrase"
    this == OnboardingStep.Security -> "Create account"
    this == OnboardingStep.Account -> "Continue"
    this == OnboardingStep.Privacy -> "Continue"
    this == OnboardingStep.Tutorial -> "Continue"
    this == OnboardingStep.Dashboard -> "Open dashboard"
    else -> "Continue"
}
