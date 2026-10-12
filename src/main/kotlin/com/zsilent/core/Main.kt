@file:Suppress("DEPRECATION")

package com.zsilent.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.core.mining.MiningController
import com.zsilent.core.core.node.NodeController
import com.zsilent.core.rpc.ZokaRpcClient
import com.zsilent.core.services.ZokaCli
import com.zsilent.core.ui.ZSilentApp
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZButtonSize
import com.zsilent.core.ui.components.ZButtonText
import com.zsilent.core.ui.components.ZButtonVariant
import com.zsilent.core.ui.theme.ZDensity
import com.zsilent.core.ui.theme.ZSilentTheme
import com.zsilent.core.ui.theme.ZTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.net.URI
import java.io.File
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption
import javax.swing.JOptionPane

/**
 * Window size for the current monitor: 80% of its width/height, clamped so
 * the window is never taller than it is wide (the app's layout assumes a
 * landscape shape — sidebar + content side by side).
 */
private fun preferredWindowSizeDp(): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> {
    val minWideAspect = 1.4f
    return try {
        val bounds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration.bounds
        val width = (bounds.width * 0.8f)
        var height = (bounds.height * 0.8f)
        if (width / height < minWideAspect) {
            height = width / minWideAspect
        }
        width.dp to height.dp
    } catch (_: Throwable) {
        1280.dp to 820.dp
    }
}

fun main() {
    val instanceLock = AppInstanceLock.acquire() ?: return
    application {
        val appState = rememberZSilentAppState()
        val (windowWidth, windowHeight) = remember { preferredWindowSizeDp() }
        val windowState = rememberWindowState(width = windowWidth, height = windowHeight)
        var windowVisible by remember { mutableStateOf(true) }

        LaunchedEffect(appState) {
            appState.start()
        }
        DisposableEffect(appState) {
            onDispose { appState.close() }
        }
        DisposableEffect(instanceLock) {
            onDispose { instanceLock.close() }
        }

        // Atender a quien intente abrir la aplicación mientras esta copia está
        // escondida en la bandeja. Sin esto, el segundo arranque moría contra
        // el candado y la aplicación parecía no abrir nunca más.
        //
        // Se borra el aviso ANTES de mostrar la ventana: si mostrar fallara, es
        // preferible perder un pedido a quedar mostrando la ventana en un bucle
        // porque el archivo nunca se limpió.
        LaunchedEffect(Unit) {
            val request = AppInstanceLock.showRequestFile()
            runCatching { request.delete() }
            while (true) {
                kotlinx.coroutines.delay(400)
                if (runCatching { request.exists() }.getOrDefault(false)) {
                    runCatching { request.delete() }
                    windowVisible = true
                    windowState.isMinimized = false
                }
            }
        }

        Tray(
            icon = painterResource("zsilent-icon.png"),
            tooltip = "ZSilent Core",
            menu = {
                Item("Open ZSilent Core", onClick = {
                    windowVisible = true
                    windowState.isMinimized = false
                })
                Item("Lock wallet", onClick = { appState.lockApp("Session closed.") })
                Separator()
                Item("Exit ZSilent Core", onClick = {
                    appState.close()
                    exitApplication()
                })
            }
        )

        if (windowVisible) {
            Window(
                onCloseRequest = { windowVisible = false },
                title = "ZSilent Core",
                state = windowState,
                undecorated = true,
                icon = painterResource("zsilent-icon.png")
            ) {
                ZSilentRoot(
                    appState = appState,
                    onMinimize = { windowState.isMinimized = true },
                    onCloseToTray = { windowVisible = false },
                    onToggleMaximize = {
                        windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    }
                )
            }
        }
    }
}

internal class AppInstanceLock(
    private val channel: FileChannel,
    private val lock: FileLock
) : AutoCloseable {
    override fun close() {
        runCatching { lock.release() }
        runCatching { channel.close() }
    }

    companion object {
        fun acquire(): AppInstanceLock? {
            val dataRoot = defaultDataRoot().apply { mkdirs() }
            val channel = FileChannel.open(
                File(dataRoot, "zsilent-core.lock").toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )
            return try {
                val lock = channel.tryLock()
                if (lock == null) {
                    channel.close()
                    requestExistingInstanceToShow()
                    null
                } else {
                    AppInstanceLock(channel, lock)
                }
            } catch (_: OverlappingFileLockException) {
                channel.close()
                requestExistingInstanceToShow()
                null
            } catch (error: Throwable) {
                channel.close()
                throw error
            }
        }

        private fun defaultDataRoot(): File {
            val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
            return if (appData != null) {
                File(appData, "ZSilent Core")
            } else {
                File(System.getProperty("user.home"), ".zsilent-core")
            }
        }

        /**
         * Archivo que la segunda instancia deja para pedirle a la primera que
         * se muestre.
         *
         * La X esconde la ventana en la bandeja en vez de cerrar el proceso, y
         * el proceso escondido se queda con el candado. Quien cerraba con la X
         * y volvía a abrir desde el icono chocaba contra un cartel de "ya está
         * corriendo": para el usuario, la aplicación simplemente no abría. Un
         * archivo alcanza para avisar y evita montar un canal entre procesos.
         */
        fun showRequestFile(): File = File(defaultDataRoot(), "zsilent-core.show")

        private fun requestExistingInstanceToShow() {
            runCatching { showRequestFile().writeText(System.currentTimeMillis().toString()) }
                .onFailure {
                    // Si ni siquiera se puede escribir el aviso, al menos decir
                    // algo: quedarse mudo es lo que hacía parecer que la
                    // aplicación estaba rota.
                    JOptionPane.showMessageDialog(
                        null,
                        "ZSilent Core is already running. Open it from the tray icon.",
                        "ZSilent Core",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
        }
    }
}

@Composable
private fun rememberZSilentAppState(): ZSilentAppState {
    val cli = remember { ZokaCli.discover() }
    val networkSettings = remember(cli.projectRoot, cli.resourcesRoot) {
        ZokaNetworkConfig.load(cli.projectRoot, cli.resourcesRoot)
    }
    val networkRpc = remember(networkSettings.remoteRpcUrl) { ZokaRpcClient(networkSettings.remoteRpcUrl) }
    val localRpc = remember(networkSettings.localRpcUrl) { ZokaRpcClient(networkSettings.localRpcUrl) }
    val nodeController = remember { NodeController(cli) }
    val miningController = remember(cli, localRpc) { MiningController(cli, localRpc) }
    return remember {
        ZSilentAppState(
            cli = cli,
            rpc = networkRpc,
            localRpc = localRpc,
            nodeController = nodeController,
            miningController = miningController
        )
    }
}

@Composable
private fun WindowScope.ZSilentRoot(
    appState: ZSilentAppState,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onCloseToTray: () -> Unit
) {
    val ui by appState.state.collectAsState()

    // `darkTheme` venía persistiéndose en PersistedSetup y viajando por todo
    // ZSilentUiState desde hace tiempo, pero nadie lo aplicaba: acá se llamaba
    // `ZSilentTheme {}` sin argumentos, así que siempre ganaba el default. La
    // preferencia existía y el valor ya estaba en alcance — solo faltaba
    // pasarlo.
    ZSilentTheme(
        darkTheme = ui.darkTheme,
        density = if (ui.compactDensity) ZDensity.Compact else ZDensity.Cozy,
        animations = ui.animationsEnabled,
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                WindowTitleBar(
                    onMinimize = onMinimize,
                    onToggleMaximize = onToggleMaximize,
                    onCloseToTray = onCloseToTray
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (ZokaNetworkConfig.isClientOutdated()) {
                        OutdatedVersionScreen()
                    } else {
                        ZSilentApp(appState)
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowScope.WindowTitleBar(
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onCloseToTray: () -> Unit
) {
    InstallWindowDragHandler()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // La red sale de la configuración y no de una constante. Estuvo escrita
        // a mano como "Zenith mainnet" y seguía diciendo mainnet corriendo en
        // testnet, que es la peor forma posible de equivocarse: el título es lo
        // primero que se mira para saber si lo que hay en pantalla es dinero.
        // Zenith paso a ser la testnet de Zoka Quantic: se mira el chain id,
        // no `Network`, que para Zenith sigue diciendo "mainnet". "Quantic" va
        // en el rosa de la marca, como en zoka.network.
        val title = "ZSilent Core · ${ZokaNetworkConfig.DisplayName} · v${ZokaNetworkConfig.AppVersion}"
        val brandAt = title.indexOf("Quantic")
        val brandColor = MaterialTheme.colorScheme.primary
        Text(
            buildAnnotatedString {
                if (brandAt < 0) {
                    append(title)
                } else {
                    append(title.substring(0, brandAt))
                    withStyle(SpanStyle(color = brandColor)) { append("Quantic") }
                    append(title.substring(brandAt + "Quantic".length))
                }
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (ZokaNetworkConfig.IsTestnet) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            WindowControlButton(onClick = onMinimize) {
                Icon(Icons.Outlined.Remove, contentDescription = "Minimize")
            }
            WindowControlButton(onClick = onToggleMaximize) {
                Icon(Icons.Outlined.CropSquare, contentDescription = "Maximize")
            }
            WindowControlButton(onClick = onCloseToTray, close = true) {
                Icon(Icons.Outlined.Close, contentDescription = "Close to tray")
            }
        }
    }
}

@Composable
private fun WindowScope.InstallWindowDragHandler() {
    DisposableEffect(window) {
        var dragOffset: Point? = null
        val listener = object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                if (event.y <= 36) {
                    dragOffset = event.point
                }
            }

            override fun mouseReleased(event: MouseEvent) {
                dragOffset = null
            }

            override fun mouseDragged(event: MouseEvent) {
                val offset = dragOffset ?: return
                window.location = Point(
                    event.xOnScreen - offset.x,
                    event.yOnScreen - offset.y
                )
            }
        }
        window.addMouseListener(listener)
        window.addMouseMotionListener(listener)
        onDispose {
            window.removeMouseListener(listener)
            window.removeMouseMotionListener(listener)
        }
    }
}

@Composable
private fun OutdatedVersionScreen() {
    val releaseUrl = "https://github.com/zoka-network/zsilent-core/releases/latest"
    val scheme = MaterialTheme.colorScheme
    // El naranja #FF6B35 anterior no existe en la paleta nueva. Esto es una
    // advertencia bloqueante, así que va en el semántico de warning.
    val warning = ZTheme.semantics.warning
    Box(
        modifier = Modifier.fillMaxSize().background(scheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            Text(
                "Versión desactualizada",
                color = warning,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Esta versión de ZSilent Core (v${ZokaNetworkConfig.AppVersion}) ya no es compatible con la red ${ZokaNetworkConfig.DisplayName}.\n\nDescargá la versión más reciente (v${ZokaNetworkConfig.MinimumClientVersion} o superior) para continuar minando y usando la red.",
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            ZButton(
                onClick = { runCatching { Desktop.getDesktop().browse(URI(releaseUrl)) } },
                variant = ZButtonVariant.Accent,
                size = ZButtonSize.Large,
            ) { ZButtonText("Descargar última versión") }
            Text(
                "v${ZokaNetworkConfig.AppVersion} · requiere v${ZokaNetworkConfig.MinimumClientVersion}+",
                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    close: Boolean = false,
    content: @Composable () -> Unit
) {
    val contentColor = if (close) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(width = 38.dp, height = 32.dp)
    ) {
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}
