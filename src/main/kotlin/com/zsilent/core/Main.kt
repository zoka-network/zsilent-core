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
import androidx.compose.ui.text.font.FontWeight
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
import com.zsilent.core.ui.theme.ZSilentTheme
import java.io.File
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption
import javax.swing.JOptionPane

fun main() {
    val instanceLock = AppInstanceLock.acquire() ?: return
    application {
        val appState = rememberZSilentAppState()
        val windowState = rememberWindowState(width = 1280.dp, height = 820.dp)
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

private class AppInstanceLock(
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
                    showAlreadyRunningMessage()
                    null
                } else {
                    AppInstanceLock(channel, lock)
                }
            } catch (_: OverlappingFileLockException) {
                channel.close()
                showAlreadyRunningMessage()
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

        private fun showAlreadyRunningMessage() {
            JOptionPane.showMessageDialog(
                null,
                "ZSilent Core is already running. Open it from the tray icon.",
                "ZSilent Core",
                JOptionPane.INFORMATION_MESSAGE
            )
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
    val miningController = remember { MiningController(cli) }
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

    ZSilentTheme(darkTheme = ui.darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                WindowTitleBar(
                    onMinimize = onMinimize,
                    onToggleMaximize = onToggleMaximize,
                    onCloseToTray = onCloseToTray
                )
                Box(modifier = Modifier.weight(1f)) {
                    ZSilentApp(appState)
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
        Text(
            "ZSilent Core",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
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
