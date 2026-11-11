package com.zsilent.core.core.storage

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.models.PersistedSetup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

class SetupStore(private val path: Path) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun load(): PersistedSetup? {
        if (!Files.exists(path)) return null
        return runCatching {
            json.decodeFromString<PersistedSetup>(Files.readString(path))
        }.getOrNull()
    }

    fun save(setup: PersistedSetup) {
        Files.createDirectories(path.parent)
        Files.writeString(path, json.encodeToString(setup))
    }

    fun clear() {
        Files.deleteIfExists(path)
    }

    companion object {
        /**
         * Cada red tiene su propio setup (`setup-quantic-testnet-v3.json`):
         * de la v2 a la v3 lo copia `NetworkMigration` (las mismas wallets);
         * de la v1 a la v2 el onboarding se hizo de nuevo.
         */
        fun default(network: String = ZokaNetworkConfig.Network): SetupStore {
            val base = System.getenv("APPDATA")
                ?.takeIf { it.isNotBlank() }
                ?.let { Path.of(it, "ZSilent Core") }
                ?: Path.of(System.getProperty("user.home"), ".zsilent-core")
            return SetupStore(base.resolve("setup-quantic-${network.ifBlank { "testnet" }}-v3.json"))
        }
    }
}
