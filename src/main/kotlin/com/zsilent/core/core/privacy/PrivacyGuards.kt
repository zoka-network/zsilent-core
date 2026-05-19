package com.zsilent.core.core.privacy

object PrivacyGuards {
    private val sensitiveWords = listOf(
        "seed",
        "mnemonic",
        "private_key",
        "spending_key",
        "view_key",
        "password"
    )

    fun redactForLog(value: String): String {
        return if (sensitiveWords.any { value.contains(it, ignoreCase = true) }) {
            "[redacted sensitive data]"
        } else {
            value
        }
    }
}

