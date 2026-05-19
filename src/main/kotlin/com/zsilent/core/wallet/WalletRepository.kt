package com.zsilent.core.wallet

class WalletRepository {
    fun mockAccount(): WalletAccount {
        return WalletAccount(
            name = "Main account",
            address = "zka1ZE9wTKvWJurUCD4F8EvikNbbYU3bmdZsx4mYUeRVjUfFG86kRUN1iardYPeMgnerKdv8NW1eHA2QVyzcm2BhDoXUaVza2",
            balance = "4,096.2500 ZKA",
            lockedBalance = "128.0000 ZKA",
            privacyMode = PrivacyMode.Silent
        )
    }

    fun mockActivity(): List<WalletActivity> {
        return listOf(
            WalletActivity("Private transfer", "Queued for node", "-200.0000 ZKA", ActivityStatus.Pending),
            WalletActivity("Mining reward", "Unlocked after maturity", "+64.0000 ZKA", ActivityStatus.Confirmed),
            WalletActivity("Receive", "Shielded address", "+1,000.0000 ZKA", ActivityStatus.Confirmed)
        )
    }

    fun mockNetwork(): NetworkStatus {
        return NetworkStatus(
            node = "Local node online",
            height = "418",
            peers = "8",
            sync = "100%"
        )
    }

    fun mockSeedWords(): List<String> {
        return listOf(
            "silent", "orbit", "velvet", "north", "matrix", "field",
            "cipher", "ember", "silver", "anchor", "logic", "harbor"
        )
    }
}
