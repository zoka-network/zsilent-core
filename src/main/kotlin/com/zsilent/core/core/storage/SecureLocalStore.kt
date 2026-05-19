package com.zsilent.core.core.storage

import java.nio.file.Path

class SecureLocalStore(private val root: Path) {
    fun rootPath(): Path = root

    fun requireEncryptedExport(password: CharArray) {
        try {
            require(password.size >= 12) { "backup password must be at least 12 characters" }
        } finally {
            password.fill('\u0000')
        }
    }
}

