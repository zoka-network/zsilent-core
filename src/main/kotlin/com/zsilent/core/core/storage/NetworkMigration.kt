package com.zsilent.core.core.storage

import java.io.File

/**
 * El protocolo v3 (revisión del 10/11/2026) es una cadena nueva: el nodo y la
 * copia de la cadena de las wallets empiezan de cero en `.zoka/quantic-<red>-v3`.
 * Lo que es del usuario pasa solo, sin onboarding ni contraseñas: sus wallets
 * (la misma frase da la misma dirección en la v3, y el nodo abre los archivos
 * de la v2) y el setup. De la v2 no se borra ni se cambia nada.
 */
object NetworkMigration {
    private const val MARKER = "migrated-from-v2"

    /**
     * Copia una sola vez de la v2 a la v3 lo que la v3 todavía no tiene y lo
     * marca. `zokaHome` es la carpeta de Zoka (`%APPDATA%\Zoka`, donde están
     * `.zoka/<red>`) y `setupRoot` la de ZSilent Core, donde están los setups.
     * Devuelve lo que copió. Un corte a mitad se retoma en el arranque
     * siguiente, con los archivos que falten; después de la marca no se copia
     * nada más, así una wallet borrada en la v3 no vuelve.
     */
    fun fromV2(zokaHome: File, setupRoot: File, network: String): List<String> {
        val net = network.ifBlank { "testnet" }
        val old = File(zokaHome, ".zoka/quantic-$net-v2")
        val new = File(zokaHome, ".zoka/quantic-$net-v3")
        val marker = File(new, MARKER)
        if (marker.exists()) return emptyList()
        val copied = mutableListOf<String>()
        val wallets = File(old, "wallets")
        if (wallets.isDirectory) {
            wallets.walkTopDown().filter { it.isFile }.forEach { file ->
                val relative = file.relativeTo(wallets).path
                val target = File(File(new, "wallets"), relative)
                if (!target.exists()) {
                    target.parentFile?.mkdirs()
                    val partial = File(target.parentFile, target.name + ".migrating")
                    file.copyTo(partial, overwrite = true)
                    if (!partial.renameTo(target)) {
                        partial.delete()
                        error("could not move ${partial.name} into place")
                    }
                    copied += "wallets/$relative"
                }
            }
        }
        val setupOld = File(setupRoot, "setup-quantic-$net-v2.json")
        val setupNew = File(setupRoot, "setup-quantic-$net-v3.json")
        if (setupOld.isFile && !setupNew.exists()) {
            setupOld.copyTo(setupNew)
            copied += setupNew.name
        }
        new.mkdirs()
        marker.writeText(copied.joinToString("\n"))
        return copied
    }
}
