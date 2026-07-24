package com.zsilent.core.core.node

import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RollingNodeLogTest {
    private fun tempLog(): File {
        val dir = Files.createTempDirectory("zsilent-node-log").toFile().apply { deleteOnExit() }
        return File(dir, "node.log")
    }

    @Test
    fun rotates_during_an_open_session_and_keeps_writing() {
        val file = tempLog()
        val log = RollingNodeLog(file, maxBytes = 180, now = { Instant.EPOCH })

        log.open()
        repeat(20) { index -> log.append("entry-$index-${"x".repeat(24)}") }
        log.close()

        val previous = File(file.parentFile, "node.log.1")
        assertTrue(previous.isFile, "the active session should rotate node.log")
        assertTrue(file.isFile, "logging should continue in a new node.log")
        assertTrue(file.length() <= 180, "the current log should stay bounded")
        assertTrue(previous.length() <= 180, "the retained previous log should stay bounded")
        assertContains(file.readText(Charsets.UTF_8), "entry-19")
        assertFalse(File(file.parentFile, "node.log.2").exists(), "only one previous log is retained")
    }

    @Test
    fun rotates_an_oversized_file_when_opening() {
        val file = tempLog().apply { writeText("old".repeat(100), Charsets.UTF_8) }
        val log = RollingNodeLog(file, maxBytes = 100, now = { Instant.EPOCH })

        log.open()
        log.append("new-session")
        log.close()

        assertTrue(File(file.parentFile, "node.log.1").length() >= 300)
        assertContains(file.readText(Charsets.UTF_8), "new-session")
    }
}
