package com.zsilent.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.zsilent.core.models.OnboardingStep
import com.zsilent.core.ui.onboarding.OnboardingShell
import com.zsilent.core.ui.onboarding.WelcomeStepNoir
import com.zsilent.core.ui.screens.IntroScreen
import com.zsilent.core.ui.screens.LoginContent
import com.zsilent.core.ui.theme.ZSilentTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/**
 * Capturas de las pantallas con la marca Zoka Quantic, dibujadas fuera de
 * pantalla: sin nodo, sin wallet y sin abrir ventanas. Sirven para revisar el
 * cambio visual sin correr la app. Solo se generan si se pide una carpeta:
 *
 *     $env:ZSILENT_SCREENSHOTS_DIR = "build/screenshots"; ./gradlew.bat test
 *
 * Sin esa variable la prueba no hace nada, para no ensuciar corridas normales.
 */
class QuanticScreenshotsTest {

    @Test
    fun renders_the_quantic_branded_screens_when_requested() {
        val dir = System.getenv("ZSILENT_SCREENSHOTS_DIR")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: return
        dir.mkdirs()
        for (dark in listOf(true, false)) {
            val theme = if (dark) "oscuro" else "claro"
            shot(dir, "onboarding-bienvenida-$theme", dark) {
                OnboardingShell(
                    step = OnboardingStep.Welcome,
                    busy = false,
                    canContinue = true,
                    primaryLabel = "Continue",
                    onBack = {},
                    onNext = {},
                    error = null,
                    canBack = false,
                ) { WelcomeStepNoir() }
            }
            shot(dir, "intro-$theme", dark) { IntroScreen(onLaunch = {}) }
            shot(dir, "login-$theme", dark) {
                LoginContent(
                    activeWalletName = "Primary",
                    activeWalletAddress = "zka1s8tnq0vzokademoaddress000000000000kdemo0",
                    wallets = listOf("Primary" to "zka1s8tnq0vzokademoaddress000000000000kdemo0"),
                    lockMessage = null,
                    loginError = null,
                    busy = false,
                    zokaReady = true,
                    onSelectWallet = {},
                    onLogin = {},
                    onRestore = {},
                    onReset = {},
                )
            }
        }
    }

    private fun shot(dir: File, name: String, dark: Boolean, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width = 1280, height = 820, density = Density(1f)) {
            ZSilentTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    content()
                }
            }
        }
        try {
            scene.render(0)
            val bytes = scene.render(2_000_000_000).encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(dir, "$name.png").writeBytes(bytes)
            assertTrue(bytes.size > 10_000, "captura vacia: $name")
        } finally {
            scene.close()
        }
    }
}
