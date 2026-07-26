package com.zsilent.core.core.storage

import com.zsilent.core.models.PersistedSetup
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the opt-in gate for the chain explorer across upgrades and restarts.
 *
 * `PersistedSetup` is rebuilt from scratch at every save site rather than
 * copied, so a field that some site forgets to carry silently reverts to its
 * default. For an opt-in flag that means a setting the user turned on can
 * disappear the next time they touch an unrelated toggle.
 */
class SetupStoreExplorerFlagTest {

    private fun storeInTempDir(): SetupStore {
        val dir = Files.createTempDirectory("zsilent-setup-test")
        dir.toFile().deleteOnExit()
        return SetupStore(dir.resolve("setup.json"))
    }

    /**
     * The upgrade path: a setup file written before the flag existed has no
     * such key. It must read as off — enabling an experimental section on
     * users who never asked for it would defeat the point of opting in.
     */
    @Test
    fun setup_file_without_the_flag_reads_as_disabled() {
        val dir = Files.createTempDirectory("zsilent-setup-legacy")
        dir.toFile().deleteOnExit()
        val path = dir.resolve("setup.json")
        Files.writeString(
            path,
            """
            {
              "onboardingComplete": true,
              "walletName": "main",
              "walletAddress": "zka1abc",
              "autoLockMinutes": 10,
              "darkTheme": true,
              "torEnabled": true
            }
            """.trimIndent()
        )

        val loaded = SetupStore(path).load()

        assertTrue(loaded != null, "legacy setup file should still load")
        assertFalse(loaded.explorerEnabled, "absent flag must not enable the section")
        assertEquals("main", loaded.walletName)
    }

    @Test
    fun enabling_the_flag_survives_a_save_and_reload() {
        val store = storeInTempDir()
        store.save(PersistedSetup(onboardingComplete = true, explorerEnabled = true))

        assertTrue(store.load()!!.explorerEnabled)
    }

    @Test
    fun disabling_the_flag_survives_a_save_and_reload() {
        val store = storeInTempDir()
        store.save(PersistedSetup(onboardingComplete = true, explorerEnabled = true))
        store.save(PersistedSetup(onboardingComplete = true, explorerEnabled = false))

        assertFalse(store.load()!!.explorerEnabled)
    }

    /**
     * A cleared setup returns null rather than a default-constructed object,
     * so the caller cannot mistake "never configured" for "explorer off".
     */
    @Test
    fun cleared_setup_loads_as_null() {
        val store = storeInTempDir()
        store.save(PersistedSetup(onboardingComplete = true, explorerEnabled = true))
        store.clear()

        assertEquals(null, store.load())
    }
}
