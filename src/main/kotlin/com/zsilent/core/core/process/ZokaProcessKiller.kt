package com.zsilent.core.core.process

import com.zsilent.core.services.ZokaCli
import java.io.File
import java.util.concurrent.TimeUnit

object ZokaProcessKiller {
    private val longRunningZokaCommands = setOf(
        "fullnode",
        "node",
        "zokahd-guided",
        "mine",
        "mine-console"
    )

    fun terminate(process: Process?, graceMillis: Long = 1_500L) {
        val handle = process?.toHandle() ?: return
        terminateHandles(listOf(handle), graceMillis)
    }

    fun terminateManagedZokaProcesses(cli: ZokaCli, graceMillis: Long = 1_500L): Int {
        val handles = ProcessHandle.allProcesses()
            .filter { handle -> handle.pid() != ProcessHandle.current().pid() }
            .filter { handle -> isManagedZokaProcess(handle, cli) }
            .toList()

        terminateHandles(handles, graceMillis)
        return handles.size
    }

    private fun terminateHandles(handles: List<ProcessHandle>, graceMillis: Long) {
        val targets = handles
            .flatMap { handle -> handle.descendants().toList() + handle }
            .distinctBy { it.pid() }
            .filter { it.isAlive }
            .filter { it.pid() != ProcessHandle.current().pid() }

        targets.forEach { target ->
            runCatching { target.destroy() }
        }
        targets.forEach { target ->
            runCatching { target.onExit().get(graceMillis, TimeUnit.MILLISECONDS) }
        }
        targets
            .filter { it.isAlive }
            .forEach { target -> runCatching { target.destroyForcibly() } }
    }

    private fun isManagedZokaProcess(handle: ProcessHandle, cli: ZokaCli): Boolean {
        val info = handle.info()
        val command = info.command().orElse("")
        val args = info.arguments().orElse(emptyArray())
        return when {
            sameFile(command, cli.zokahd) -> true
            sameFile(command, cli.zoka) -> args.firstOrNull() in longRunningZokaCommands
            else -> false
        }
    }

    private fun sameFile(command: String, expected: File?): Boolean {
        if (expected == null || command.isBlank()) return false
        return runCatching {
            File(command).toPath().toRealPath() == expected.toPath().toRealPath()
        }.getOrElse {
            File(command).absolutePath.normalizePath() == expected.absolutePath.normalizePath()
        }
    }

    private fun String.normalizePath(): String {
        return replace('/', File.separatorChar)
            .replace('\\', File.separatorChar)
            .trim()
            .lowercase()
    }
}
