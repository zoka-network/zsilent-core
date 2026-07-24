package com.zsilent.core.core.node

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A small, synchronous rolling log for the managed node process.
 *
 * Rotation happens while the process is running. The previous implementation
 * checked the size only when a node session opened, which allowed one long
 * session to grow node.log without bound.
 */
internal class RollingNodeLog(
    private val file: File,
    private val maxBytes: Long,
    private val now: () -> Instant = Instant::now
) : AutoCloseable {
    private var writer: BufferedWriter? = null
    private var bytesWritten: Long = 0

    init {
        require(maxBytes > 0) { "maxBytes must be positive" }
    }

    @Synchronized
    fun open() {
        close()
        file.parentFile?.mkdirs()
        if (file.isFile && file.length() >= maxBytes) {
            rotateClosedFile()
        }
        writer = newWriter()
        bytesWritten = file.takeIf { it.isFile }?.length() ?: 0
    }

    @Synchronized
    fun append(line: String) {
        if (writer == null) return
        val record = "${TIMESTAMP.format(now())} $line\n"
        val recordBytes = record.toByteArray(StandardCharsets.UTF_8).size.toLong()

        if (bytesWritten > 0 && bytesWritten + recordBytes > maxBytes) {
            if (!rotateOpenFile()) return
        }

        try {
            writer?.write(record)
            // Preserve the last diagnostic lines even if the node crashes.
            writer?.flush()
            bytesWritten += recordBytes
        } catch (_: Exception) {
            close()
        }
    }

    @Synchronized
    override fun close() {
        try {
            writer?.flush()
            writer?.close()
        } catch (_: Exception) {
            // Logging is diagnostic only; shutdown must keep progressing.
        }
        writer = null
    }

    private fun rotateOpenFile(): Boolean {
        try {
            writer?.flush()
            writer?.close()
        } catch (_: Exception) {
            writer = null
            return false
        }
        writer = null

        if (!rotateClosedFile()) {
            // Keep appending to the original file if Windows temporarily
            // refuses the move. Losing diagnostics is worse than oversizing.
            writer = runCatching { newWriter() }.getOrNull()
            bytesWritten = file.takeIf { it.isFile }?.length() ?: 0
            return writer != null
        }

        return runCatching {
            writer = newWriter()
            bytesWritten = 0
            true
        }.getOrDefault(false)
    }

    private fun rotateClosedFile(): Boolean = runCatching {
        if (!file.exists()) return@runCatching true
        val previous = File(file.parentFile, "${file.name}.1")
        java.nio.file.Files.move(
            file.toPath(),
            previous.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
        true
    }.getOrDefault(false)

    private fun newWriter(): BufferedWriter = BufferedWriter(
        OutputStreamWriter(FileOutputStream(file, true), StandardCharsets.UTF_8)
    )

    private companion object {
        val TIMESTAMP: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
    }
}
