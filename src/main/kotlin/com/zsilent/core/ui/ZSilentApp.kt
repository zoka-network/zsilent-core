package com.zsilent.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zsilent.core.models.AppSection
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.ui.screens.DashboardScreen
import com.zsilent.core.ui.screens.ExplorerScreen
import com.zsilent.core.ui.screens.LoginScreen
import com.zsilent.core.ui.screens.LogsScreen
import com.zsilent.core.ui.screens.MiningPoolScreen
import com.zsilent.core.ui.screens.MiningScreen
import com.zsilent.core.ui.screens.NodeScreen
import com.zsilent.core.ui.screens.OnboardingScreen
import com.zsilent.core.ui.screens.P2PExchangeScreen
import com.zsilent.core.ui.screens.PrivacyScreen
import com.zsilent.core.ui.screens.ReceiveScreen
import com.zsilent.core.ui.screens.SendScreen
import com.zsilent.core.ui.screens.SettingsScreen
import com.zsilent.core.ui.screens.WalletScreen

// ── Sidebar color palette ("Zenith") ────────────────────────────────────────
private val SbBg       = Color(0xFF0A0A0C)   // sidebar background — near black
private val SbBtn      = Color(0xFF050506)   // nav button bg — true near-black
private val SbBtnHover = Color(0xFF1D1F24)   // hover — a touch lighter than bg
private val SbBtnActive= Color(0xFF000000)   // active — deepest black
private val SbText     = Color(0xFFFFFFFF)   // button text + icons — WHITE
private val SbMuted    = Color(0xFF7A7F88)   // toggle / muted elements
private val SbAccent   = Color(0xFFA8E6B0)   // active accent — pastel green
private val SbDivider  = Color(0xFF2A2D33)   // separator

@Composable
fun ZSilentApp(appState: ZSilentAppState) {
    val uiState by appState.state.collectAsState()

    if (!uiState.onboardingComplete) { OnboardingScreen(uiState, appState); return }
    if (uiState.appLocked) { LoginScreen(uiState, appState); return }

    var expanded by remember { mutableStateOf(true) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (expanded) 220.dp else 80.dp,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "sidebarWidth"
    )

    Row(Modifier.fillMaxSize().background(Color(0xFF2B2D31))) {
        CollapsibleSidebar(
            selected = uiState.selectedSection,
            onSelect = appState::select,
            expanded = expanded,
            onToggle = { expanded = !expanded },
            onCloseSession = { appState.lockApp("Session closed.") },
            sections = visibleSections(uiState),
            modifier = Modifier.width(sidebarWidth)
        )
        // 1dp separator line between sidebar and content
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF45474D)))

        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2B2D31))) {
            when (uiState.selectedSection) {
                AppSection.Dashboard -> DashboardScreen(uiState, appState)
                AppSection.Wallet    -> WalletScreen(uiState, appState)
                AppSection.Send      -> SendScreen(uiState, appState)
                AppSection.Receive   -> ReceiveScreen(uiState, appState)
                AppSection.Node      -> NodeScreen(uiState, appState)
                AppSection.Mining    -> MiningScreen(uiState, appState)
                AppSection.MiningPool  -> MiningPoolScreen(uiState, appState)
                AppSection.P2PExchange -> P2PExchangeScreen(uiState, appState)
                AppSection.Explorer    ->
                    // Reachable only while the opt-in holds; if it is switched
                    // off from another surface the router must not keep
                    // rendering a section the sidebar no longer lists.
                    if (uiState.explorerEnabled) ExplorerScreen(uiState, appState)
                    else DashboardScreen(uiState, appState)
                AppSection.Privacy   -> PrivacyScreen(uiState, appState)
                AppSection.Logs      -> LogsScreen(uiState)
                AppSection.Settings  -> SettingsScreen(uiState, appState)
            }
        }
    }
}

/**
 * Sections offered in the sidebar for the current settings.
 *
 * Experimental sections are opt-in, so they are filtered out of the nav rather
 * than shown disabled — a visible entry that refuses to open reads as a bug.
 */
private fun visibleSections(uiState: ZSilentUiState): List<AppSection> =
    AppSection.entries.filter { section ->
        when (section) {
            AppSection.Explorer -> uiState.explorerEnabled
            else -> true
        }
    }

@Composable
private fun CollapsibleSidebar(
    selected: AppSection,
    onSelect: (AppSection) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCloseSession: () -> Unit,
    sections: List<AppSection>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(SbBg)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Logo + Toggle ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            if (expanded) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.zsilent.core.ui.onboarding.NoirBrandBadge(size = 32.dp)
                    Text("ZSilent", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                com.zsilent.core.ui.onboarding.NoirBrandBadge(size = 32.dp)
            }
            ToggleSidebarButton(expanded = expanded, onClick = onToggle)
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(SbDivider))
        Spacer(Modifier.height(4.dp))

        // ── Nav items ─────────────────────────────────────────────────
        // Scrollable and weighted so it only ever claims the space left
        // over after the logo/toggle above and "Close session"/version
        // below — with 12 sections the list no longer always fits a
        // shorter window, and without this the overflowing items were
        // simply clipped with no way to reach them.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEach { section ->
                val (label, icon) = section.navItem()
                SidebarItem(
                    icon = icon,
                    label = label,
                    selected = selected == section,
                    expanded = expanded,
                    onClick = { onSelect(section) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SbDivider))
        Spacer(Modifier.height(4.dp))

        // ── Close session ─────────────────────────────────────────────
        SidebarItem(
            icon = Icons.AutoMirrored.Outlined.Logout,
            label = "Close session",
            selected = false,
            expanded = expanded,
            onClick = onCloseSession,
            tintOverride = Color(0xFFFFFFFF)   // white icon on black button
        )

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(
                "v${com.zsilent.core.config.ZokaNetworkConfig.AppVersion}",
                color = SbMuted,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun ToggleSidebarButton(expanded: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(if (hovered) SbBtnHover else Color.Transparent, RoundedCornerShape(8.dp))
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (expanded) Icons.AutoMirrored.Outlined.KeyboardArrowLeft
                          else Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse sidebar" else "Expand sidebar",
            tint = SbMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    tintOverride: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    // Nav buttons are ALWAYS black — hover darkens slightly, active deepens
    val bgColor = when {
        selected -> SbBtnActive
        hovered  -> SbBtnHover
        else     -> SbBtn
    }
    val iconTint  = tintOverride ?: SbText   // always white
    val textColor = tintOverride ?: SbText   // always white
    val itemShape = RoundedCornerShape(10.dp)

    val elevation by animateFloatAsState(
        targetValue = if (selected) 4f else if (hovered) 2f else 0f,
        animationSpec = tween(150),
        label = "itemElevation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .graphicsLayer { shadowElevation = elevation; clip = true }
            .background(bgColor, itemShape)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = if (expanded) 12.dp else 0.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
    ) {
        // Active indicator dot
        if (selected && expanded) {
            Box(Modifier.size(4.dp).background(SbAccent, CircleShape))
            Spacer(Modifier.width(8.dp))
        }

        Icon(icon, contentDescription = label, tint = if (selected) SbAccent else iconTint, modifier = Modifier.size(24.dp))

        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Text(label, color = textColor, fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1)
        }
    }
}

private data class NavItem(val label: String, val icon: ImageVector)

private fun AppSection.navItem(): NavItem = when (this) {
    AppSection.Dashboard -> NavItem("Dashboard", Icons.Outlined.Dashboard)
    AppSection.Wallet    -> NavItem("Wallet",    Icons.Outlined.AccountBalanceWallet)
    AppSection.Send      -> NavItem("Send",      Icons.AutoMirrored.Outlined.Send)
    AppSection.Receive   -> NavItem("Receive",   Icons.Outlined.QrCode)
    AppSection.Node      -> NavItem("Node",      Icons.Outlined.Memory)
    AppSection.Mining    -> NavItem("Mining",    Icons.Outlined.Bolt)
    AppSection.MiningPool  -> NavItem("Mining Pool",  Icons.Outlined.Groups)
    AppSection.P2PExchange -> NavItem("P2P Exchange", Icons.Outlined.CurrencyExchange)
    AppSection.Explorer    -> NavItem("Explorer",     Icons.Outlined.Explore)
    AppSection.Privacy   -> NavItem("Privacy",   Icons.Outlined.Security)
    AppSection.Logs      -> NavItem("Logs",      Icons.Outlined.Terminal)
    AppSection.Settings  -> NavItem("Settings",  Icons.Outlined.Settings)
}
