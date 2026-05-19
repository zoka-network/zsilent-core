package com.zsilent.core.wallet

data class WalletAccount(
    val name: String,
    val address: String,
    val balance: String,
    val lockedBalance: String,
    val privacyMode: PrivacyMode
)

data class WalletActivity(
    val title: String,
    val subtitle: String,
    val amount: String,
    val status: ActivityStatus
)

enum class ActivityStatus {
    Confirmed,
    Pending,
    Failed
}

enum class PrivacyMode(val label: String) {
    Standard("Standard"),
    Private("Private"),
    Silent("Silent")
}

data class NetworkStatus(
    val node: String,
    val height: String,
    val peers: String,
    val sync: String
)

data class SendDraft(
    val recipient: String = "",
    val amount: String = "",
    val fee: String = "0.0000 ZKA",
    val privacyMode: PrivacyMode = PrivacyMode.Silent,
    val confirmOpen: Boolean = false
)
