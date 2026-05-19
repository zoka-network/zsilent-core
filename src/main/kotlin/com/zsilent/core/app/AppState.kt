package com.zsilent.core.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zsilent.core.wallet.PrivacyMode
import com.zsilent.core.wallet.SendDraft
import com.zsilent.core.wallet.WalletRepository

class AppState(
    private val repository: WalletRepository = WalletRepository()
) {
    val account = repository.mockAccount()
    val activity = repository.mockActivity()
    val network = repository.mockNetwork()
    val seedWords = repository.mockSeedWords()

    var onboardingComplete by mutableStateOf(false)
        private set
    var currentScreen: Screen by mutableStateOf(Screen.Welcome)
        private set
    var walletName by mutableStateOf("Main account")
        private set
    var importedSeed by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var passwordConfirm by mutableStateOf("")
        private set
    var balancesHidden by mutableStateOf(false)
        private set
    var localNode by mutableStateOf(true)
        private set
    var privateMode by mutableStateOf(true)
        private set
    var seedCheckA by mutableStateOf("")
        private set
    var seedCheckB by mutableStateOf("")
        private set
    var sendDraft by mutableStateOf(SendDraft())
        private set

    fun navigate(screen: Screen) {
        currentScreen = screen
    }

    fun nextOnboarding() {
        val index = onboardingScreens.indexOf(currentScreen).coerceAtLeast(0)
        currentScreen = onboardingScreens.getOrElse(index + 1) { Screen.OnboardingDone }
    }

    fun previousOnboarding() {
        val index = onboardingScreens.indexOf(currentScreen).coerceAtLeast(0)
        currentScreen = onboardingScreens.getOrElse(index - 1) { Screen.Welcome }
    }

    fun finishOnboarding() {
        onboardingComplete = true
        currentScreen = Screen.Dashboard
    }

    fun updateWalletName(value: String) {
        walletName = value
    }

    fun updateImportedSeed(value: String) {
        importedSeed = value
    }

    fun updatePassword(value: String) {
        password = value
    }

    fun updatePasswordConfirm(value: String) {
        passwordConfirm = value
    }

    fun updateBalancesHidden(value: Boolean) {
        balancesHidden = value
    }

    fun updateLocalNode(value: Boolean) {
        localNode = value
    }

    fun updatePrivateMode(value: Boolean) {
        privateMode = value
    }

    fun updateSeedCheckA(value: String) {
        seedCheckA = value.trim().lowercase()
    }

    fun updateSeedCheckB(value: String) {
        seedCheckB = value.trim().lowercase()
    }

    fun updateSendDraft(
        recipient: String = sendDraft.recipient,
        amount: String = sendDraft.amount,
        fee: String = sendDraft.fee,
        privacyMode: PrivacyMode = sendDraft.privacyMode,
        confirmOpen: Boolean = sendDraft.confirmOpen
    ) {
        sendDraft = SendDraft(
            recipient = recipient,
            amount = amount,
            fee = fee,
            privacyMode = privacyMode,
            confirmOpen = confirmOpen
        )
    }

    fun seedVerified(): Boolean {
        val words = activeSeedWords()
        return words.size >= 9 && seedCheckA == words[2] && seedCheckB == words[8]
    }

    fun passwordValid(): Boolean {
        return password.length >= 8 && password == passwordConfirm
    }

    fun activeSeedWords(): List<String> {
        val importedWords = importedSeed
            .trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        return if (importedWords.size in setOf(12, 24)) importedWords else seedWords
    }
}
