package com.zsilent.core.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.zsilent.core.models.OnboardingStep
import com.zsilent.core.ui.components.*
import com.zsilent.core.ui.onboarding.OnboardingShell
import com.zsilent.core.ui.onboarding.ScreenBody
import com.zsilent.core.ui.onboarding.ScreenEyebrow
import com.zsilent.core.ui.onboarding.ScreenTitle
import com.zsilent.core.ui.onboarding.WelcomeStepNoir
import com.zsilent.core.ui.theme.ZDensity
import com.zsilent.core.ui.theme.ZSilentTheme
import com.zsilent.core.ui.theme.ZTheme
import com.zsilent.core.ui.theme.zGrid
import com.zsilent.core.ui.theme.zNoise
import com.zsilent.core.ui.theme.zScanOffset

// ─────────────────────────────────────────────────────────────────────────────
//  Galería de verificación visual.
//
//  Entry point aparte de la app real: no arranca nodo, no toca la wallet y no
//  pasa por login. Sirve para mirar el resultado de cada etapa del rediseño sin
//  montar todo el runtime.
//
//      ./gradlew.bat runGallery
//
//  Tiene los tres interruptores que hacen falta para verificar las decisiones
//  D2 y D4: tema claro/oscuro, densidad y animaciones.
//
//  No se empaqueta en el MSI — solo la usa la tarea de Gradle.
// ─────────────────────────────────────────────────────────────────────────────

fun main() = application {
    var dark by remember { mutableStateOf(true) }
    var density by remember { mutableStateOf(ZDensity.Cozy) }
    var animations by remember { mutableStateOf(true) }
    var view by remember { mutableStateOf(GalleryView.Primitives) }
    var onboardingStep by remember { mutableStateOf(OnboardingStep.Welcome) }

    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 1280.dp, height = 900.dp),
        title = "ZSilent — galería de componentes",
    ) {
        ZSilentTheme(darkTheme = dark, density = density, animations = animations) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (view) {
                    GalleryView.Primitives -> Box(
                        Modifier
                            .fillMaxSize()
                            .zNoise(MaterialTheme.colorScheme.onSurface)
                            .zGrid(MaterialTheme.colorScheme.onSurface, alpha = 0.03f)
                    ) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                .padding(ZTheme.spacing.screen),
                            verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap * 2),
                        ) {
                            Controls(
                                dark = dark, onDark = { dark = it },
                                density = density, onDensity = { density = it },
                                animations = animations, onAnimations = { animations = it },
                                view = view, onView = { view = it },
                            )
                            PaletteSection()
                            TypographySection()
                            ButtonsSection()
                            InputsSection()
                            StatusSection()
                            DataSection()
                            StatesSection()
                        }
                    }

                    // El onboarding real ocupa toda la ventana, así que se
                    // muestra completo con su propio shell. Solo el paso Welcome
                    // no necesita ZSilentUiState, así que es el único que se
                    // puede renderizar suelto; el resto se recorre con las
                    // flechas para ver el cromo (cabecera, progreso, pie).
                    GalleryView.Onboarding -> OnboardingPreview(
                        step = onboardingStep,
                        onStep = { onboardingStep = it },
                        onExit = { view = GalleryView.Primitives },
                    )
                }
            }
        }
    }
}

private enum class GalleryView { Primitives, Onboarding }

@Composable
private fun OnboardingPreview(
    step: OnboardingStep,
    onStep: (OnboardingStep) -> Unit,
    onExit: () -> Unit,
) {
    val steps = OnboardingStep.entries
    Box(Modifier.fillMaxSize()) {
        OnboardingShell(
            step = step,
            busy = false,
            canContinue = true,
            primaryLabel = "Continue",
            error = null,
            canBack = step != OnboardingStep.Welcome,
            onBack = { onStep(steps[(step.ordinal - 1).coerceAtLeast(0)]) },
            onNext = { onStep(steps[(step.ordinal + 1).coerceAtMost(steps.lastIndex)]) },
        ) {
            if (step == OnboardingStep.Welcome) {
                WelcomeStepNoir()
            } else {
                ScreenEyebrow(step.name)
                ScreenTitle("Paso ${step.name}")
                ScreenBody(
                    "Este paso necesita ZSilentUiState, así que en la galería solo se " +
                        "muestra el cromo del shell: cabecera, progreso y pie. Para verlo " +
                        "con datos reales hay que correr la app."
                )
            }
        }
        ZButton(
            onClick = onExit,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            variant = ZButtonVariant.Outline,
            size = ZButtonSize.Small,
        ) { ZButtonText("Volver") }
    }
}

@Composable
private fun Controls(
    dark: Boolean, onDark: (Boolean) -> Unit,
    density: ZDensity, onDensity: (ZDensity) -> Unit,
    animations: Boolean, onAnimations: (Boolean) -> Unit,
    view: GalleryView, onView: (GalleryView) -> Unit,
) {
    Panel(title = "Galería", meta = "etapa 1") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZLabel("Ver")
            ZButton(
                onClick = { onView(GalleryView.Onboarding) },
                variant = ZButtonVariant.Accent,
                size = ZButtonSize.Small,
            ) { ZButtonText("Onboarding") }
        }
        ThinDivider()
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZLabel("Tema oscuro")
                ZToggle(checked = dark, onCheckedChange = onDark)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZLabel("Animaciones")
                ZToggle(checked = animations, onCheckedChange = onAnimations)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZLabel("Densidad")
                SegmentedSelector(
                    options = listOf("compact", "cozy"),
                    selected = if (density == ZDensity.Compact) "compact" else "cozy",
                    onSelect = { onDensity(if (it == "compact") ZDensity.Compact else ZDensity.Cozy) },
                    modifier = Modifier.width(200.dp),
                )
            }
        }
    }
}

@Composable
private fun PaletteSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
        SectionTitle(
            eyebrow = "Fundaciones",
            title = "Paleta",
            sub = "En oscuro, los tres primeros rellenos son casi idénticos a propósito: " +
                "la separación la produce el borde, no el relleno.",
        )
        val scheme = MaterialTheme.colorScheme
        val sem = ZTheme.semantics
        val swatches = listOf(
            "background" to scheme.background,
            "surface" to scheme.surface,
            "surfaceHigh" to scheme.surfaceContainerHigh,
            "elevated" to sem.elevated,
            "border" to scheme.outlineVariant,
            "borderStrong" to sem.borderStrong,
            "onSurface" to scheme.onSurface,
            "onSurfaceVariant" to scheme.onSurfaceVariant,
            "primary" to scheme.primary,
            "success" to sem.success,
            "warning" to sem.warning,
            "danger" to sem.danger,
            "info" to sem.info,
        )
        Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                swatches.forEach { (name, color) ->
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(color, MaterialTheme.shapes.extraSmall)
                                .border(BorderStroke(1.dp, scheme.outlineVariant), MaterialTheme.shapes.extraSmall)
                        )
                        Text(name, style = ZTheme.text.pill, color = scheme.onSurfaceVariant)
                        Text(color.hex(), style = ZTheme.text.pill, color = scheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

private fun Color.hex(): String {
    val argb = toArgb()
    return "#%02X%02X%02X".format((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
}

@Composable
private fun TypographySection() {
    Column(verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
        SectionTitle(
            eyebrow = "Fundaciones",
            title = "Tipografía",
            sub = "Inter para interfaz, JetBrains Mono para datos. Los montos usan cifras " +
                "tabulares: las columnas de abajo tienen que alinearse dígito a dígito.",
        )
        Panel {
            Text("Operational state at a glance", style = MaterialTheme.typography.headlineMedium)
            Text("Balances, sending and receiving.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ThinDivider()
            Text("EYEBROW · CONTROL CENTER", style = ZTheme.text.eyebrow, color = MaterialTheme.colorScheme.primary)
            Text("PANEL LABEL", style = ZTheme.text.panelLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("FIELD LABEL", style = ZTheme.text.fieldLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("NAV GROUP", style = ZTheme.text.navGroup, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ThinDivider()
            // Prueba de cifras tabulares: estos tres tienen que alinearse.
            Text("1,284.220000 ZKA", style = ZTheme.text.mono)
            Text("1,111.111111 ZKA", style = ZTheme.text.mono)
            Text("9,000.000009 ZKA", style = ZTheme.text.mono)
            ThinDivider()
            Text("1,284.220000", style = ZTheme.text.monoDisplay)
        }
    }
}

@Composable
private fun ButtonsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
        SectionTitle(eyebrow = "Controles", title = "Botones")
        Panel {
            ZLabel("Variantes")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZButton(onClick = {}, variant = ZButtonVariant.Accent) { ZButtonText("Accent") }
                ZButton(onClick = {}, variant = ZButtonVariant.Default) { ZButtonText("Default") }
                ZButton(onClick = {}, variant = ZButtonVariant.Outline) { ZButtonText("Outline") }
                ZButton(onClick = {}, variant = ZButtonVariant.Ghost) { ZButtonText("Ghost") }
                ZButton(onClick = {}, variant = ZButtonVariant.Danger) { ZButtonText("Danger") }
            }
            ZLabel("Tamaños")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ZButton(onClick = {}, variant = ZButtonVariant.Accent, size = ZButtonSize.Small) { ZButtonText("Small") }
                ZButton(onClick = {}, variant = ZButtonVariant.Accent, size = ZButtonSize.Medium) { ZButtonText("Medium") }
                ZButton(onClick = {}, variant = ZButtonVariant.Accent, size = ZButtonSize.Large) { ZButtonText("Large") }
            }
            ZLabel("Con icono, y deshabilitado")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZButton(onClick = {}, variant = ZButtonVariant.Accent) {
                    Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(14.dp))
                    ZButtonText("Start node")
                }
                ZButton(onClick = {}, variant = ZButtonVariant.Outline) {
                    Icon(Icons.Outlined.Bolt, null, modifier = Modifier.size(14.dp))
                    ZButtonText("Start miner")
                }
                ZButton(onClick = {}, variant = ZButtonVariant.Accent, enabled = false) { ZButtonText("Disabled") }
            }
            ZLabel("Copiar — confirma 1,2 s")
            CopyButton(value = "zka1s8tnq0vzokademoaddress000000000000kdemo0")
        }
    }
}

@Composable
private fun InputsSection() {
    var text by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("1284.220000") }
    var password by remember { mutableStateOf("") }
    var toggle by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
        SectionTitle(eyebrow = "Controles", title = "Campos")
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ZLabel("Destination address")
                ZInput(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "zka1…",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ZLabel("Amount (ZKA)")
                ZInput(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    trailing = { ZButton(onClick = {}, size = ZButtonSize.Small) { ZButtonText("Max") } },
                )
            }
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Session password",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ZToggle(checked = toggle, onCheckedChange = { toggle = it })
                Text("Tor transport", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StatusSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
        SectionTitle(
            eyebrow = "Estado",
            title = "Pills y medidores",
            sub = "Los puntos de on, warn y err laten. Apagá el interruptor de animaciones " +
                "de arriba y tienen que quedar quietos.",
        )
        Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("node · online", ZTone.On)
                Pill("tor · starting", ZTone.Warn)
                Pill("node · error", ZTone.Err)
                Pill("mining · off", ZTone.Off)
                Pill("local-first", ZTone.Info)
                Pill("default", ZTone.Default)
            }
            ThinDivider()
            ZLabel("Sync progress")
            MeterBar(fraction = 0.72f)
            ZLabel("Barrido — se usa al verificar un pago por Tor")
            ScanBarDemo()
        }
    }
}

/** Muestra `zs-scan`: una barra que cruza su contenedor. */
@Composable
private fun ScanBarDemo() {
    val offset = zScanOffset()
    val trackWidth = 120.dp
    val barWidth = 40.dp
    Box(
        Modifier
            .width(trackWidth)
            .height(8.dp)
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
    ) {
        Box(
            Modifier
                .offset(x = (trackWidth - barWidth) * ((offset + 1f) / 2f))
                .width(barWidth)
                .height(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

@Composable
private fun DataSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
        SectionTitle(eyebrow = "Datos", title = "Métricas y filas")
        Row(horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap), modifier = Modifier.fillMaxWidth()) {
            Panel(modifier = Modifier.weight(1f)) {
                PanelHeader(
                    title = "Network mesh",
                    subtitle = "Peers conectados y altura de red",
                    right = { Pill("6 peers", ZTone.On) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
                    Stat("Local height", "12,943", modifier = Modifier.weight(1f))
                    Stat("Network height", "12,943", tone = ZTone.Info, modifier = Modifier.weight(1f))
                    Stat("Difficulty", "1,048,576", modifier = Modifier.weight(1f))
                    Stat("Pending", "3", hint = "mempool", modifier = Modifier.weight(1f))
                }
                ThinDivider()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZLabel("Endpoint")
                    Truncate("ca7721e9f3b48d0a1c5e7f9b2d4a6c8e0f1a3b5c7d9e", head = 12, tail = 6)
                }
                KeyValueRow("Local RPC", "127.0.0.1:3000")
                CopyableKeyValueRow("Onion address", "zokaseed1abcdefghijklmnop.onion")
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
                MetricTile(
                    label = "Hashrate",
                    value = "3,740 H/s",
                    caption = "4 de 8 hilos",
                    accent = MetricAccent.Green,
                    fraction = 0.5f,
                )
                MetricTile(
                    label = "Team hashrate",
                    value = "— H/s",
                    caption = "todavía no medido",
                    accent = MetricAccent.Neutral,
                    dimmed = true,
                )
                StatusCard(title = "Emission", value = "155,316 ZKA", detail = "acumulada")
            }
        }
    }
}

@Composable
private fun StatesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
        SectionTitle(eyebrow = "Estados", title = "Vacío, error y QR")
        Row(horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap), modifier = Modifier.fillMaxWidth()) {
            Panel(modifier = Modifier.weight(1f)) {
                EmptyState(
                    text = "No peers yet",
                    sub = "Arrancá el nodo desde Dashboard o Node para abrir conexiones.",
                )
                ErrorText("Passwords do not match")
                HintText("Available 1,284.220000 ZKA")
            }
            Panel(modifier = Modifier.weight(1f), contentPadding = PaddingValues(ZTheme.spacing.panel)) {
                PanelHeader(title = "Receive", right = { Pill("private", ZTone.On) })
                QrCode("zka1s8tnq0vzokademoaddress000000000000kdemo0")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
