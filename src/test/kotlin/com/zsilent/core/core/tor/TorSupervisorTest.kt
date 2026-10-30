package com.zsilent.core.core.tor

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TorSupervisorTest {

    private fun tempDir(prefix: String): File =
        Files.createTempDirectory(prefix).toFile().apply { deleteOnExit() }

    @Test
    fun torrc_has_managed_socks_and_hidden_service_lines() {
        val dataDir = tempDir("tor-data")
        val hsDir = tempDir("tor-hs")
        val torrc = TorSupervisor.buildTorrcContent(
            dataDir = dataDir,
            hiddenServiceDir = hsDir,
            config = TorLaunchConfig(),
            bundleDir = null
        )

        // Managed SOCKS port is 19050 to avoid clashing with Tor Browser's 9050.
        assertContains(torrc, "SocksPort 127.0.0.1:19050")
        assertContains(torrc, "HiddenServiceVersion 3")
        // The virtual port is the node's P2P port on the Quantic testnet.
        assertContains(torrc, "HiddenServicePort 28180 127.0.0.1:28180")
        assertContains(torrc, "DataDirectory ${dataDir.absolutePath.replace("\\", "/")}")
        assertContains(torrc, "HiddenServiceDir ${hsDir.absolutePath.replace("\\", "/")}")
        // No geoip bundle provided -> no GeoIP lines (still a valid torrc).
        assertFalse(torrc.contains("GeoIPFile"), "GeoIPFile must be omitted when no bundle dir is given")
    }

    @Test
    fun torrc_references_geoip_when_present_in_bundle() {
        val dataDir = tempDir("tor-data")
        val hsDir = tempDir("tor-hs")
        val bundleDir = tempDir("tor-bundle")
        File(bundleDir, "geoip").writeText("dummy-geoip")
        File(bundleDir, "geoip6").writeText("dummy-geoip6")

        val torrc = TorSupervisor.buildTorrcContent(
            dataDir = dataDir,
            hiddenServiceDir = hsDir,
            config = TorLaunchConfig(),
            bundleDir = bundleDir
        )

        assertContains(torrc, "GeoIPFile ${File(bundleDir, "geoip").absolutePath.replace("\\", "/")}")
        assertContains(torrc, "GeoIPv6File ${File(bundleDir, "geoip6").absolutePath.replace("\\", "/")}")
    }

    @Test
    fun torrc_omits_geoip_lines_when_bundle_lacks_the_files() {
        val dataDir = tempDir("tor-data")
        val hsDir = tempDir("tor-hs")
        val emptyBundle = tempDir("tor-bundle-empty")

        val torrc = TorSupervisor.buildTorrcContent(
            dataDir = dataDir,
            hiddenServiceDir = hsDir,
            config = TorLaunchConfig(),
            bundleDir = emptyBundle
        )

        assertFalse(torrc.contains("GeoIPFile"), "GeoIPFile must be omitted when the file is absent")
        assertFalse(torrc.contains("GeoIPv6File"), "GeoIPv6File must be omitted when the file is absent")
    }

    @Test
    fun torrc_quotes_paths_that_contain_spaces() {
        val parent = tempDir("tor space test")
        val dataDir = File(parent, "data with space").apply { mkdirs() }
        val hsDir = File(parent, "hidden service").apply { mkdirs() }

        val torrc = TorSupervisor.buildTorrcContent(
            dataDir = dataDir,
            hiddenServiceDir = hsDir,
            config = TorLaunchConfig(),
            bundleDir = null
        )

        // Paths with spaces must be quoted so Tor parses them as a single arg.
        assertTrue(
            torrc.lines().any { it.startsWith("DataDirectory \"") && it.endsWith("\"") },
            "DataDirectory with a space must be quoted"
        )
    }

    @Test
    fun a_client_tor_for_tor_mode_has_a_socks_port_and_no_onion_service() {
        val dataDir = tempDir("tor-client-data")
        val torrc = TorSupervisor.buildTorrcContent(
            dataDir = dataDir,
            hiddenServiceDir = tempDir("tor-client-hs"),
            config = TorLaunchConfig(socksPort = 19050, hiddenService = false),
            bundleDir = null
        )
        assertTrue(torrc.lines().any { it == "SocksPort 127.0.0.1:19050" }, torrc)
        assertTrue(torrc.lines().any { it.startsWith("DataDirectory ") }, torrc)
        assertTrue(torrc.lines().none { it.startsWith("HiddenService") }, torrc)
        assertTrue(torrc.lines().any { it == "Log notice stdout" }, torrc)
    }
}
