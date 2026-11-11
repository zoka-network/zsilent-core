package com.zsilent.core.core.wallet

/** Export starts with the least disclosure; a full history is always explicit. */
enum class ViewKeyScope(val wireName: String, val label: String, val description: String) {
    Incoming(
        "incoming", "Incoming view key",
        "Shows received payments and memos. It cannot see spending or your balance, and cannot spend. " +
            "Change from older wallet versions may appear as a received payment."
    ),
    Full(
        "full", "Full view key",
        "Shows received and sent payments and your balance. It cannot spend."
    );

    fun exportFlags(): List<String> = if (this == Incoming) listOf("--incoming") else emptyList()

    /** Never silently export a full key if a bundled node is too old. */
    internal fun validateExport(kind: String, key: String): String {
        val suffix = if (this == Incoming) "viewin2" else "view2"
        require((kind == wireName || (this == Full && kind.isBlank())) &&
            listOf("zq", "zqtest", "zqregtest").any { key.startsWith(it + suffix) }) {
            "The node returned a different view key type. Update ZSilent and try again."
        }
        return key
    }
}
