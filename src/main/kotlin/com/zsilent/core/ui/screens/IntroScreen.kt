package com.zsilent.core.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.ui.components.Pill
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZButtonSize
import com.zsilent.core.ui.components.ZButtonText
import com.zsilent.core.ui.components.ZButtonVariant
import com.zsilent.core.ui.components.ZTone
import com.zsilent.core.ui.theme.ZTheme
import com.zsilent.core.ui.theme.zGrid
import com.zsilent.core.ui.theme.zNoise
import com.zsilent.core.ui.theme.zPulseAlpha
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  Intro — portado de zoka-whisper-desk/src/components/zs/Intro.tsx
//
//  Ver docs/GUIA_CAMBIO_VISUAL.md, etapa 2.
//
//  Se muestra UNA SOLA VEZ, en la primera corrida tras instalar. La decisión
//  está tomada: en cada arranque cansaría. El control es `introSeen` en
//  PersistedSetup — ver la nota de defaults asimétricos que hay ahí.
//
//  Las seis líneas de arranque son una secuencia fija con retardo, no reflejan
//  el arranque real del nodo: en este punto ni siquiera hay sesión desbloqueada,
//  así que no hay nada real que informar. Por eso el pie dice `standby` y no
//  inventa métricas.
// ─────────────────────────────────────────────────────────────────────────────

private data class BootLine(val tag: String, val message: String)

private val BOOT_LINES = listOf(
    BootLine("init", "loading zsilent runtime"),
    BootLine("keys", "sealed keystore · aes-256-gcm"),
    BootLine("core", "local node controller attached"),
    BootLine("tor", "onion transport · standby"),
    BootLine("p2p", "peer discovery armed"),
    BootLine("ui", "render surface · local-first"),
)

@Composable
fun IntroScreen(onLaunch: () -> Unit) {
    var shown by remember { mutableStateOf(0) }
    val ready = shown >= BOOT_LINES.size
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(shown) {
        if (shown < BOOT_LINES.size) {
            delay(260L + (shown * 40L))
            shown += 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .zNoise(scheme.onSurface)
            .zGrid(scheme.onSurface, alpha = 0.03f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 980.dp).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .alpha(zPulseAlpha())
                        .background(ZTheme.semantics.success, CircleShape)
                )
                Text(
                    "zsilent · local process",
                    style = ZTheme.text.mono,
                    color = scheme.onSurfaceVariant
                )
            }

            // Ventana dentro de la ventana, como en el prototipo.
            val shape = MaterialTheme.shapes.medium
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(scheme.surface.copy(alpha = 0.6f), shape)
                    .border(BorderStroke(1.dp, scheme.outlineVariant), shape)
            ) {
                TitleBar()
                HorizontalDivider(color = scheme.outlineVariant)
                Row(Modifier.fillMaxWidth()) {
                    BrandColumn(
                        ready = ready,
                        onLaunch = onLaunch,
                        modifier = Modifier.weight(1.1f)
                    )
                    Box(Modifier.width(1.dp).fillMaxHeight().background(scheme.outlineVariant))
                    BootColumn(shown = shown, ready = ready, modifier = Modifier.weight(0.9f))
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "© zoka network",
                    style = ZTheme.text.mono,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    "v${ZokaNetworkConfig.AppVersion}",
                    style = ZTheme.text.mono,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun TitleBar() {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                ZTheme.semantics.danger,
                ZTheme.semantics.warning,
                ZTheme.semantics.success,
            ).forEach { color ->
                Box(Modifier.size(9.dp).background(color.copy(alpha = 0.8f), CircleShape))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Outlined.Terminal, null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Text(
                "ZSILENT CORE",
                style = ZTheme.text.navGroup,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun BrandColumn(ready: Boolean, onLaunch: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.zsilent.core.ui.onboarding.NoirBrandBadge(size = 40.dp)
            Column {
                Text("ZSilent", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                Text(
                    "ZOKA NETWORK · LOCAL CLIENT",
                    style = ZTheme.text.navGroup,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Text(
            "A silent runtime\nfor private value.",
            style = MaterialTheme.typography.displaySmall,
            color = scheme.onSurface
        )

        Text(
            "ZSilent runs a local node, holds its own keys and speaks a zero-knowledge " +
                "protocol straight to the mesh. No servers, no accounts, no telemetry.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp)
        )

        val badges = listOf(
            Icons.Outlined.Lock to "sealed vault",
            Icons.Outlined.VpnKey to "self-custody",
            Icons.Outlined.Bolt to "local miner",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            badges.forEach { (icon, label) -> BootBadge(icon, label, Modifier.weight(1f)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ZButton(
                onClick = onLaunch,
                variant = ZButtonVariant.Accent,
                size = ZButtonSize.Large,
                enabled = ready,
            ) {
                ZButtonText(if (ready) "Launch ZSilent" else "Booting")
                if (ready) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward, null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                "shown once, on first run",
                style = ZTheme.text.mono,
                color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BootBadge(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Column(
        modifier
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.5f), shape)
            .border(BorderStroke(1.dp, scheme.outlineVariant), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = scheme.onSurface, modifier = Modifier.size(14.dp))
        Text(label.uppercase(), style = ZTheme.text.navGroup, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun BootColumn(shown: Int, ready: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxHeight()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "stdout · boot",
                style = ZTheme.text.mono,
                color = scheme.onSurfaceVariant
            )
            Pill(if (ready) "ready" else "booting", if (ready) ZTone.On else ZTone.Warn)
        }
        HorizontalDivider(color = scheme.outlineVariant)

        Column(
            Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BOOT_LINES.take(shown).forEachIndexed { index, line ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "[%02d]".format(index + 1),
                        style = ZTheme.text.mono,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        line.tag.uppercase(),
                        modifier = Modifier.width(38.dp),
                        style = ZTheme.text.mono,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        line.message,
                        modifier = Modifier.weight(1f),
                        style = ZTheme.text.mono,
                        color = scheme.onSurface
                    )
                    Text(
                        "ok",
                        style = ZTheme.text.mono,
                        color = ZTheme.semantics.success
                    )
                }
            }
            if (ready) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = scheme.outlineVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    "✓ runtime online — awaiting operator",
                    style = ZTheme.text.mono,
                    color = ZTheme.semantics.success
                )
            }
        }

        HorizontalDivider(color = scheme.outlineVariant)
        // `standby` y no métricas: en este punto no hay sesión desbloqueada, así
        // que no hay nada real que informar. Inventar números acá sería
        // exactamente lo que la leyenda del Mining Pool existe para evitar.
        Row(Modifier.fillMaxWidth()) {
            listOf("node" to "standby", "tor" to "standby", "vault" to "sealed")
                .forEachIndexed { index, (label, value) ->
                    if (index > 0) {
                        Box(Modifier.width(1.dp).height(44.dp).background(scheme.outlineVariant))
                    }
                    Column(
                        Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            label.uppercase(),
                            style = ZTheme.text.navGroup,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            value,
                            style = ZTheme.text.mono,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
        }
    }
}
