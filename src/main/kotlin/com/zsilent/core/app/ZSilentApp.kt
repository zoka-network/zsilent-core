package com.zsilent.core.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zsilent.core.design.ZBackground
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSidebar
import com.zsilent.core.ui.components.ZTopBar
import com.zsilent.core.ui.screens.dashboard.DashboardScreen
import com.zsilent.core.ui.screens.dashboard.ExplorerScreen
import com.zsilent.core.ui.screens.dashboard.HistoryScreen
import com.zsilent.core.ui.screens.dashboard.ReceiveScreen
import com.zsilent.core.ui.screens.dashboard.SendScreen
import com.zsilent.core.ui.screens.dashboard.SettingsScreen
import com.zsilent.core.ui.screens.onboarding.CreateAccountScreen
import com.zsilent.core.ui.screens.onboarding.CreateOrImportScreen
import com.zsilent.core.ui.screens.onboarding.CustodyWarningScreen
import com.zsilent.core.ui.screens.onboarding.OnboardingDoneScreen
import com.zsilent.core.ui.screens.onboarding.PasswordScreen
import com.zsilent.core.ui.screens.onboarding.PrivacyModeScreen
import com.zsilent.core.ui.screens.onboarding.SeedRevealScreen
import com.zsilent.core.ui.screens.onboarding.SeedVerifyScreen
import com.zsilent.core.ui.screens.welcome.WelcomeScreen

@Composable
fun ZSilentApp() {
    val appState = remember { AppState() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZBackground)
    ) {
        if (!appState.onboardingComplete) {
            OnboardingHost(appState)
        } else {
            DashboardHost(appState)
        }
    }
}

@Composable
private fun OnboardingHost(appState: AppState) {
    Box(Modifier.fillMaxSize().padding(ZSpacing.xxl)) {
        ZCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ZSpacing.xl)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(ZSpacing.lg)
            ) {
                when (appState.currentScreen) {
                    Screen.Welcome -> WelcomeScreen(appState)
                    Screen.CustodyWarning -> CustodyWarningScreen(appState)
                    Screen.CreateOrImport -> CreateOrImportScreen(appState)
                    Screen.SeedReveal -> SeedRevealScreen(appState)
                    Screen.SeedVerify -> SeedVerifyScreen(appState)
                    Screen.Password -> PasswordScreen(appState)
                    Screen.CreateAccount -> CreateAccountScreen(appState)
                    Screen.PrivacyMode -> PrivacyModeScreen(appState)
                    Screen.OnboardingDone -> OnboardingDoneScreen(appState)
                    else -> Text("Continue setup")
                }
            }
        }
    }
}

@Composable
private fun DashboardHost(appState: AppState) {
    Row(Modifier.fillMaxSize()) {
        ZSidebar(
            selected = appState.currentScreen,
            onSelect = appState::navigate
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ZSpacing.xl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(ZSpacing.lg)
        ) {
            ZTopBar(
                title = appState.currentScreen.title,
                subtitle = "Zoka private desktop wallet"
            )
            when (appState.currentScreen) {
                Screen.Dashboard -> DashboardScreen(appState)
                Screen.Send -> SendScreen(appState)
                Screen.Receive -> ReceiveScreen(appState)
                Screen.History -> HistoryScreen(appState)
                Screen.Explorer -> ExplorerScreen(appState)
                Screen.Settings -> SettingsScreen(appState)
                else -> DashboardScreen(appState)
            }
        }
    }
}
