package com.zsilent.core.core.storage

import com.zsilent.core.models.PersistedSetup
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SetupStoreStratumSettingsTest {

    // Un setup de antes de la 3.0.2 no tiene las claves: el Stratum arranca
    // apagado y, si se enciende, solo para esta PC.
    @Test
    fun a_setup_from_before_the_stratum_keeps_it_off() {
        val dir = Files.createTempDirectory("zsilent-stratum-old")
        val path = dir.resolve("setup.json")
        Files.writeString(path, """{ "onboardingComplete": true, "explorerEnabled": true }""")

        val loaded = assertNotNull(SetupStore(path).load())

        assertTrue(loaded.onboardingComplete)
        assertFalse(loaded.stratumEnabled)
        assertFalse(loaded.stratumAllowLan)
    }

    @Test
    fun the_stratum_choice_survives_a_restart() {
        val dir = Files.createTempDirectory("zsilent-stratum-save")
        val store = SetupStore(dir.resolve("setup.json"))
        store.save(PersistedSetup(onboardingComplete = true, stratumEnabled = true, stratumAllowLan = true))

        val loaded = assertNotNull(store.load())

        assertTrue(loaded.stratumEnabled)
        assertTrue(loaded.stratumAllowLan)
    }
}
