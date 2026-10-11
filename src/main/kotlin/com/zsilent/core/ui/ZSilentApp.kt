package com.zsilent.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zsilent.core.models.AppSection
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.ui.components.Pill
import com.zsilent.core.ui.components.Truncate
import com.zsilent.core.ui.components.ZTone
import com.zsilent.core.ui.theme.ZAnimatedSwap
import com.zsilent.core.ui.theme.ZEasing
import com.zsilent.core.ui.theme.ZTheme
import com.zsilent.core.ui.theme.zGrid
import com.zsilent.core.ui.theme.zNoise
import com.zsilent.core.ui.theme.zPulseAlpha
import com.zsilent.core.ui.screens.DashboardScreen
import com.zsilent.core.ui.screens.ExplorerScreen
import com.zsilent.core.ui.screens.IntroScreen
import com.zsilent.core.ui.screens.LoginScreen
import com.zsilent.core.ui.screens.LogsScreen
import com.zsilent.core.ui.screens.MiningPoolScreen
import com.zsilent.core.ui.screens.MiningScreen
import com.zsilent.core.ui.screens.NodeScreen
import com.zsilent.core.ui.screens.OnboardingScreen
import com.zsilent.core.ui.screens.PrivacyScreen
import com.zsilent.core.ui.screens.SettingsScreen
import com.zsilent.core.ui.screens.WalletScreen

// ─────────────────────────────────────────────────────────────────────────────
//  Shell — portado de zoka-whisper-desk/src/components/zs/AppShell.tsx
//
//  Ver docs/GUIA_CAMBIO_VISUAL.md, etapa 4.
//
//  Sin colores literales: todo sale del tema. Este archivo tenía 12.
// ─────────────────────────────────────────────────────────────────────────────

/** Grupos de navegación del prototipo. */
private enum class NavGroup(val label: String) {
    Core("Core"),
    Network("Network"),
    Insight("Insight"),
    System("System"),
}

private val AppSection.group: NavGroup
    get() = when (this) {
        AppSection.Dashboard, AppSection.Wallet -> NavGroup.Core
        AppSection.Node, AppSection.Mining, AppSection.MiningPool -> NavGroup.Network
        AppSection.Explorer, AppSection.Privacy, AppSection.Logs -> NavGroup.Insight
        AppSection.Settings -> NavGroup.System
    }

@Composable
fun ZSilentApp(appState: ZSilentAppState) {
    val uiState by appState.state.collectAsState()

    // Primera corrida tras instalar: el Intro va antes de todo, incluso del
    // onboarding. Se marca visto al pulsar Launch y no vuelve a aparecer.
    if (!uiState.introSeen) { IntroScreen(onLaunch = appState::markIntroSeen); return }
    if (!uiState.onboardingComplete) { OnboardingScreen(uiState, appState); return }
    if (uiState.appLocked) { LoginScreen(uiState, appState); return }

    var expanded by remember { mutableStateOf(true) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (expanded) 256.dp else 72.dp,
        animationSpec = tween(280, easing = ZEasing),
        label = "sidebarWidth"
    )
    val scheme = MaterialTheme.colorScheme

    Row(Modifier.fillMaxSize().background(scheme.background)) {
        CollapsibleSidebar(
            selected = uiState.selectedSection,
            onSelect = appState::select,
            expanded = expanded,
            onToggle = { expanded = !expanded },
            onCloseSession = { appState.lockApp("Session closed.") },
            sections = visibleSections(uiState),
            activeWalletName = uiState.activeWalletName,
            activeWalletAddress = uiState.activeWalletAddress,
            nodeRunning = uiState.nodeStatus == ServiceStatus.Online,
            miningRunning = uiState.miningStatus == ServiceStatus.Online,
            poolRunning = uiState.teamMiningHashing,
            modifier = Modifier.width(sidebarWidth)
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(scheme.outlineVariant))

        Column(Modifier.weight(1f).fillMaxHeight()) {
            TopBar(
                section = uiState.selectedSection,
                ui = uiState,
                onToggleTheme = { appState.setDarkTheme(!uiState.darkTheme) },
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .zNoise(scheme.onSurface)
                    .zGrid(scheme.onSurface, alpha = 0.02f)
            ) {
                // El prototipo reanima el contenido al cambiar de sección
                // (`zs-in` con la sección como clave).
                ZAnimatedSwap(uiState.selectedSection) { section ->
                    when (section) {
                        AppSection.Dashboard -> DashboardScreen(uiState, appState)
                        AppSection.Wallet    -> WalletScreen(uiState, appState)
                        AppSection.Node      -> NodeScreen(uiState, appState)
                        AppSection.Mining    -> MiningScreen(uiState, appState)
                        // Las tres secciones opcionales son opt-in. Si el
                        // interruptor se apaga desde otra superficie, el router
                        // no puede seguir renderizando una sección que la
                        // sidebar ya no lista.
                        AppSection.MiningPool  ->
                            if (uiState.teamMiningEnabled) MiningPoolScreen(uiState, appState)
                            else DashboardScreen(uiState, appState)
                        AppSection.Explorer    ->
                            if (uiState.explorerEnabled) ExplorerScreen(uiState, appState)
                            else DashboardScreen(uiState, appState)
                        AppSection.Privacy   -> PrivacyScreen(uiState, appState)
                        AppSection.Logs      -> LogsScreen(uiState, appState)
                        AppSection.Settings  -> SettingsScreen(uiState, appState)
                    }
                }
            }
        }
    }
}

/**
 * Barra superior del prototipo: breadcrumb `/ sección` en mono, pills de estado
 * y el interruptor de tema.
 *
 * `mining` era un solo indicador y mentía: el minado solo y el de equipo son
 * excluyentes, así que con el pool trabajando el único indicador se veía
 * apagado. Ahora hay uno para cada uno, y sólo uno puede estar en verde.
 *
 * Las pills viven en una fila con scroll horizontal: son seis y la ventana se
 * puede achicar, y prefiero que se desplacen antes de que la última quede
 * cortada por la mitad.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopBar(
    section: AppSection,
    ui: ZSilentUiState,
    onToggleTheme: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Alto minimo, no fijo: con seis pills y una ventana angosta
                // hacen falta dos lineas, y una barra de alto fijo las recorta.
                .heightIn(min = 56.dp)
                .background(scheme.background.copy(alpha = 0.92f))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "/ ${section.name.lowercase()}",
                style = ZTheme.text.mono,
                color = scheme.onSurfaceVariant,
            )
            // Las pills bajan de linea en vez de desplazarse: con scroll
            // horizontal la ultima quedaba cortada contra el interruptor de
            // tema, y nadie descubre que ahi se puede arrastrar.
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Etiquetas cortas a proposito. Son seis: con "node · online"
                // y "pool · hashing" completos no entran ni en 1360 px, y el
                // color ya dice el estado. La palabra queda para lo que el
                // color no distingue.
                Pill("node ${ui.nodeStatus.shortLabel()}", ui.nodeStatus.tone())
                Pill(
                    "tor ${if (ui.torEnabled) "on" else "off"}",
                    if (ui.torEnabled) ZTone.On else ZTone.Off
                )
                Pill("solo ${ui.miningStatus.shortLabel()}", ui.miningStatus.tone())
                // El pool distingue "encendido" de "entregando shares": el worker
                // se pausa al publicar una solución, y un verde fijo taparía esa
                // diferencia justo cuando interesa verla.
                Pill(
                    "pool " + when {
                        ui.teamMiningHashing -> "live"
                        ui.teamMiningActive -> "idle"
                        else -> "off"
                    },
                    when {
                        ui.teamMiningHashing -> ZTone.On
                        ui.teamMiningActive -> ZTone.Warn
                        else -> ZTone.Off
                    }
                )
                Pill(
                    "expl ${if (ui.explorerEnabled) "on" else "off"}",
                    if (ui.explorerEnabled) ZTone.On else ZTone.Off
                )
            }
            ThemeToggleButton(darkTheme = ui.darkTheme, onClick = onToggleTheme)
        }
        HorizontalDivider(color = scheme.outlineVariant)
    }
}

private fun ServiceStatus.pillLabel(): String = name.lowercase()

/** Version corta para la barra superior, donde compiten seis pills por el ancho. */
private fun ServiceStatus.shortLabel(): String = when (this) {
    ServiceStatus.Online -> "on"
    ServiceStatus.Offline -> "off"
    ServiceStatus.Starting -> "warm"
    ServiceStatus.Stopping -> "stop"
    ServiceStatus.Error -> "err"
}

private fun ServiceStatus.tone(): ZTone = when (this) {
    ServiceStatus.Online -> ZTone.On
    ServiceStatus.Starting, ServiceStatus.Stopping -> ZTone.Warn
    ServiceStatus.Error -> ZTone.Err
    ServiceStatus.Offline -> ZTone.Off
}

@Composable
private fun ThemeToggleButton(darkTheme: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(
                if (hovered) scheme.surfaceContainerHigh else Color.Transparent,
                MaterialTheme.shapes.extraSmall
            )
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            contentDescription = if (darkTheme) "Switch to light theme" else "Switch to dark theme",
            tint = if (hovered) scheme.onSurface else scheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
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
            AppSection.MiningPool -> uiState.teamMiningEnabled
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
    activeWalletName: String,
    activeWalletAddress: String,
    nodeRunning: Boolean,
    miningRunning: Boolean,
    poolRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(scheme.surface.copy(alpha = 0.92f))
    ) {
        // ── Cabecera de 56dp, igual que el topbar ─────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                com.zsilent.core.ui.onboarding.NoirBrandBadge(size = 28.dp)
                if (expanded) {
                    Column {
                        Text(
                            "ZSilent",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                        Text(
                            "ZOKA · v${com.zsilent.core.config.ZokaNetworkConfig.AppVersion}",
                            style = ZTheme.text.navGroup,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (expanded) ToggleSidebarButton(expanded = true, onClick = onToggle)
        }
        HorizontalDivider(color = scheme.outlineVariant)

        if (!expanded) {
            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                ToggleSidebarButton(expanded = false, onClick = onToggle)
            }
        }

        // ── Ítems, agrupados ──────────────────────────────────────────
        // Con scroll y peso para que solo reclame el espacio que sobra: con
        // doce secciones la lista no siempre entra en una ventana baja, y sin
        // esto los ítems de más quedaban recortados sin forma de alcanzarlos.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            NavGroup.entries.forEach { group ->
                val items = sections.filter { it.group == group }
                if (items.isEmpty()) return@forEach
                if (expanded) {
                    Text(
                        group.label.uppercase(),
                        style = ZTheme.text.navGroup,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp)
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                }
                items.forEach { section ->
                    val (label, icon) = section.navItem()
                    SidebarItem(
                        icon = icon,
                        label = label,
                        selected = selected == section,
                        expanded = expanded,
                        onClick = { onSelect(section) },
                        // El punto se enciende cuando la seccion esta haciendo
                        // algo, no cuando esta habilitada. Para el pool eso es
                        // entregar shares: "tengo equipo" no es "esta minando".
                        live = when (section) {
                            AppSection.Node -> nodeRunning
                            AppSection.Mining -> miningRunning
                            AppSection.MiningPool -> poolRunning
                            // Explorer no lleva punto: estar
                            // habilitada no es estar haciendo algo, y un verde
                            // permanente no informa nada.
                            else -> false
                        },
                    )
                }
            }
        }

        HorizontalDivider(color = scheme.outlineVariant)

        // ── Wallet activa y cierre de sesión ──────────────────────────
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (expanded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(scheme.surfaceContainerHigh.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                        .border(BorderStroke(1.dp, scheme.outlineVariant), MaterialTheme.shapes.small)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ACTIVE WALLET",
                            style = ZTheme.text.navGroup,
                            color = scheme.onSurfaceVariant
                        )
                        Pill("Unlocked", ZTone.On)
                    }
                    Text(
                        activeWalletName.ifBlank { "Main wallet" },
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface
                    )
                    if (activeWalletAddress.isNotBlank()) {
                        Truncate(
                            activeWalletAddress,
                            head = 8, tail = 6,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
            SidebarItem(
                icon = Icons.AutoMirrored.Outlined.Logout,
                label = "Close session",
                selected = false,
                expanded = expanded,
                onClick = onCloseSession,
                danger = true,
            )
        }
    }
}

@Composable
private fun ToggleSidebarButton(expanded: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(
                if (hovered) scheme.surfaceContainerHigh else Color.Transparent,
                MaterialTheme.shapes.extraSmall
            )
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (expanded) Icons.AutoMirrored.Outlined.KeyboardArrowLeft
                          else Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse sidebar" else "Expand sidebar",
            tint = if (hovered) scheme.onSurface else scheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Ítem de navegación del prototipo: sin relleno propio salvo el activo, que
 * lleva `accent` al 10% y una barra de 2×16dp pegada al borde izquierdo.
 *
 * `live` enciende el punto que late a la derecha — el prototipo lo usa en Node
 * cuando está ONLINE y en Mining cuando está RUNNING.
 */
@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    live: Boolean = false,
    danger: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val scheme = MaterialTheme.colorScheme
    val dangerColor = ZTheme.semantics.danger

    val bgColor = when {
        selected -> scheme.primary.copy(alpha = 0.10f)
        hovered  -> scheme.surfaceContainerHigh
        else     -> Color.Transparent
    }
    val contentColor = when {
        danger && hovered -> dangerColor
        selected -> scheme.onSurface
        hovered  -> scheme.onSurface
        else     -> scheme.onSurfaceVariant
    }
    val itemShape = MaterialTheme.shapes.small

    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor, itemShape)
                .hoverable(interactionSource)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(horizontal = if (expanded) 10.dp else 0.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) scheme.primary else contentColor,
                modifier = Modifier.size(16.dp)
            )
            if (expanded) {
                Spacer(Modifier.width(10.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (live) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .alpha(zPulseAlpha())
                            .background(ZTheme.semantics.success, CircleShape)
                    )
                }
            }
        }
        // Barra de acento del ítem activo, pegada al borde izquierdo.
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .size(width = 2.dp, height = 16.dp)
                    .background(scheme.primary, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
            )
        }
    }
}

private data class NavItem(val label: String, val icon: ImageVector)

private fun AppSection.navItem(): NavItem = when (this) {
    AppSection.Dashboard -> NavItem("Dashboard", Icons.Outlined.Dashboard)
    AppSection.Wallet    -> NavItem("Wallet",    Icons.Outlined.AccountBalanceWallet)
    AppSection.Node      -> NavItem("Node",      Icons.Outlined.Memory)
    AppSection.Mining    -> NavItem("Mining",    Icons.Outlined.Bolt)
    AppSection.MiningPool  -> NavItem("Mining Pool",  Icons.Outlined.Groups)
    AppSection.Explorer    -> NavItem("Explorer",     Icons.Outlined.Explore)
    AppSection.Privacy   -> NavItem("Privacy",   Icons.Outlined.Security)
    AppSection.Logs      -> NavItem("Logs",      Icons.Outlined.Terminal)
    AppSection.Settings  -> NavItem("Settings",  Icons.Outlined.Settings)
}
