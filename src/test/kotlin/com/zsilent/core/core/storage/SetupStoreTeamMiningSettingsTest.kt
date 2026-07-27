package com.zsilent.core.core.storage

import com.zsilent.core.models.PersistedSetup
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetupStoreTeamMiningSettingsTest {

    @Test
    fun legacy_setup_stays_opted_out_of_team_mining() {
        val dir = Files.createTempDirectory("zsilent-team-mining-legacy")
        val path = dir.resolve("setup.json")
        Files.writeString(path, """{"onboardingComplete":true}""")

        assertFalse(SetupStore(path).load()!!.teamMiningEnabled)
    }

    @Test
    fun team_mining_opt_in_survives_restart() {
        val dir = Files.createTempDirectory("zsilent-team-mining-settings")
        val store = SetupStore(dir.resolve("setup.json"))
        store.save(
            PersistedSetup(
                onboardingComplete = true,
                teamMiningEnabled = true
            )
        )

        assertTrue(store.load()!!.teamMiningEnabled)
    }
}
