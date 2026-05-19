package com.zsilent.core.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val title: String,
    val icon: ImageVector? = null
) {
    data object Welcome : Screen("Welcome")
    data object CustodyWarning : Screen("Self custody")
    data object CreateOrImport : Screen("Wallet")
    data object SeedReveal : Screen("Seed")
    data object SeedVerify : Screen("Verify")
    data object Password : Screen("Password")
    data object CreateAccount : Screen("Create account")
    data object PrivacyMode : Screen("Privacy")
    data object OnboardingDone : Screen("Done")

    data object Dashboard : Screen("Dashboard", Icons.Outlined.Dashboard)
    data object Send : Screen("Send", Icons.AutoMirrored.Outlined.Send)
    data object Receive : Screen("Receive", Icons.Outlined.QrCode)
    data object History : Screen("History", Icons.Outlined.History)
    data object Explorer : Screen("Explorer", Icons.Outlined.Explore)
    data object Settings : Screen("Settings", Icons.Outlined.Settings)
}

val dashboardScreens = listOf(
    Screen.Dashboard,
    Screen.Send,
    Screen.Receive,
    Screen.History,
    Screen.Explorer,
    Screen.Settings
)

val onboardingScreens = listOf(
    Screen.Welcome,
    Screen.CustodyWarning,
    Screen.CreateOrImport,
    Screen.SeedReveal,
    Screen.SeedVerify,
    Screen.Password,
    Screen.CreateAccount,
    Screen.PrivacyMode,
    Screen.OnboardingDone
)
