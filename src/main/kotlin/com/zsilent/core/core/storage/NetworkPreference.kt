package com.zsilent.core.core.storage

import java.nio.file.Files
import java.nio.file.Path

/**
 * A qué red se conecta ZSilent: `mainnet` o `testnet`.
 *
 * Vive en su propio archivo, y no dentro de `setup.json`, por una razón de
 * orden de arranque: [com.zsilent.core.config.ZokaNetworkConfig] resuelve el
 * manifiesto la primera vez que alguien lo toca, y eso pasa antes de que exista
 * el estado de la aplicación que sabe leer `setup.json`. Un archivo de una sola
 * línea se puede leer desde cualquier punto sin arrastrar dependencias.
 *
 * El cambio exige reiniciar a propósito. Cada red tiene su propia cadena, su
 * propio directorio de datos y sus propias carteras; cambiarla con el nodo
 * corriendo dejaría procesos apuntando a mitades distintas.
 */
object NetworkPreference {
    private const val MAINNET = "mainnet"
    private const val TESTNET = "testnet"

    private val path: Path by lazy {
        val base = System.getenv("APPDATA")
            ?.takeIf { it.isNotBlank() }
            ?.let { Path.of(it, "ZSilent Core") }
            ?: Path.of(System.getProperty("user.home"), ".zsilent-core")
        base.resolve("network.pref")
    }

    /** La red elegida. Ante cualquier duda, mainnet: es lo que el usuario ya tenía. */
    fun read(): String = runCatching {
        if (!Files.exists(path)) return@runCatching MAINNET
        val value = Files.readString(path).trim().lowercase()
        if (value == TESTNET) TESTNET else MAINNET
    }.getOrDefault(MAINNET)

    fun write(network: String) {
        val normalized = if (network.trim().equals(TESTNET, ignoreCase = true)) TESTNET else MAINNET
        runCatching {
            Files.createDirectories(path.parent)
            Files.writeString(path, normalized)
        }
    }

    fun isTestnet(): Boolean = read() == TESTNET
}
