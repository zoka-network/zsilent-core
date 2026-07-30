import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
}

group = "com.zsilent"
val releaseVersion = "1.9.6"
version = releaseVersion

val hostOs = OperatingSystem.current()
val nodeRepoRoot = (findProperty("zoka.nodeRepoRoot") as String?)
    ?.let { layout.projectDirectory.dir(it) }
    ?: layout.projectDirectory.dir("..")
val binaryExt = if (hostOs.isWindows) ".exe" else ""
val zokaExe = nodeRepoRoot.file("target/release/zoka$binaryExt")
val zokahdExe = nodeRepoRoot.file("target/release/zokahd$binaryExt")
val networkManifest = nodeRepoRoot.file("docs/mainnet-network.v1.json")
val trustedSetupDir = nodeRepoRoot.dir("trusted-setup")
val packageResourcesRoot = layout.buildDirectory.dir("zsilent-package-resources")
val torBundleDir = (findProperty("zsilent.torBundleDir") as String?)
    ?.let { file(it) }
    ?: nodeRepoRoot.dir("third_party/tor/windows-x86_64").asFile
val requireTorBundle = (findProperty("zsilent.requireTorBundle") as String?)
    ?.toBooleanStrictOrNull()
    // A Windows release without the managed Tor payload silently loses onion
    // reachability. Require it by default; local developers can still opt out
    // explicitly with -Pzsilent.requireTorBundle=false.
    ?: hostOs.isWindows
val windowsMsvcRuntimeDlls = listOf(
    "concrt140.dll",
    "msvcp140.dll",
    "msvcp140_1.dll",
    "msvcp140_2.dll",
    "msvcp140_atomic_wait.dll",
    "msvcp140_codecvt_ids.dll",
    "vcruntime140.dll",
    "vcruntime140_1.dll"
)
val windowsSystem32 = if (hostOs.isWindows) {
    System.getenv("WINDIR")
        ?.let { file("$it/System32") }
        ?: file("C:/Windows/System32")
} else {
    null
}

val prepareZSilentRuntimeResources by tasks.registering(Copy::class) {
    val requiredFiles = listOf(zokaExe, zokahdExe, networkManifest)
    val requiredTrustedSetupFiles = listOf(
        "DIGEST",
        "balance.params",
        "merkle.params",
        "nullifier.params",
        "private_tx.params"
    )
    doFirst {
        delete(packageResourcesRoot)
        val missing = requiredFiles
            .map { it.asFile }
            .filterNot { it.exists() && it.isFile }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing ZSilent runtime file(s): ${missing.joinToString { it.path }}. " +
                    "Build the Rust node first with: cargo build --release --features randomx --bins"
            )
        }
        val missingTrustedSetup = requiredTrustedSetupFiles
            .map { trustedSetupDir.file(it).asFile }
            .filterNot { it.exists() && it.isFile }
        if (missingTrustedSetup.isNotEmpty()) {
            throw GradleException(
                "Missing ZSilent trusted setup file(s): ${missingTrustedSetup.joinToString { it.path }}. " +
                    "Regenerate trusted setup before packaging."
            )
        }
        if (hostOs.isWindows) {
            val torExeCandidates = listOf(
                File(torBundleDir, "tor.exe"),
                File(torBundleDir, "Tor/tor.exe"),
                File(torBundleDir, "tor/tor.exe"),
                File(torBundleDir, "bin/tor.exe")
            )
            val hasTor = torExeCandidates.any { it.isFile }
            if (requireTorBundle && !hasTor) {
                throw GradleException(
                    "Missing Tor Expert Bundle at ${torBundleDir.absolutePath}. " +
                        "Prepare it first or pass -Pzsilent.torBundleDir=<dir>."
                )
            }
            if (!hasTor) {
                logger.warn("Tor Expert Bundle not found at ${torBundleDir.absolutePath}; managed Tor will require ZSILENT_TOR_BIN in this build.")
            }
        }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    into(packageResourcesRoot.map { it.dir("common") })
    from(zokaExe) { into("bin") }
    from(zokahdExe) { into("bin") }
    if (hostOs.isWindows) {
        from(windowsMsvcRuntimeDlls.mapNotNull { dll -> windowsSystem32?.resolve(dll) }.filter { it.isFile }) {
            into("bin")
        }
        if (torBundleDir.exists()) {
            from(torBundleDir) {
                into("tor/windows-x86_64")
            }
        }
    }
    from(networkManifest) { into("docs") }
    from(trustedSetupDir) { into("trusted-setup") }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.ktor:ktor-client-cio:3.0.1")
    implementation("io.ktor:ktor-client-websockets:3.0.1")

    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.zsilent.core.MainKt"
        jvmArgs += listOf(
            "-Dzsilent.version=$releaseVersion",
            // DNS seed, not a hardcoded IP -- the old DigitalOcean seed IP was
            // decommissioned 2026-07-09; DNS lets the seed host rotate without
            // a rebuild.
            "-Dzsilent.bootstrapMultiaddr=/dns4/seed1.zoka.network/tcp/28080"
        )

        nativeDistributions {
            val formats = when {
                hostOs.isWindows -> arrayOf(TargetFormat.Msi, TargetFormat.Exe)
                hostOs.isLinux -> arrayOf(TargetFormat.Deb, TargetFormat.Rpm)
                hostOs.isMacOsX -> arrayOf(TargetFormat.Dmg, TargetFormat.Pkg)
                else -> arrayOf(TargetFormat.Msi)
            }
            targetFormats(*formats)
            packageName = "ZSilent Core"
            packageVersion = releaseVersion
            description = "Private desktop control center for the Zoka network."
            vendor = "ZSilent"
            appResourcesRootDir.set(packageResourcesRoot)

            windows {
                menu = true
                shortcut = true
                dirChooser = true
                perUserInstall = true
                iconFile.set(project.file("src/main/resources/zsilent-icon.ico"))
                upgradeUuid = "63E8B74E-A8E8-49F0-9E52-9DF7B48C6F95"
            }

            linux {
                packageName = "zsilent-core"
                menuGroup = "Network"
            }

            macOS {
                bundleID = "com.zsilent.core"
            }
        }
    }
}

// Galería de verificación visual del rediseño (docs/GUIA_CAMBIO_VISUAL.md).
// Entry point aparte: no arranca nodo, no toca la wallet y no pasa por login,
// así se puede mirar el resultado de cada etapa sin montar todo el runtime.
tasks.register<JavaExec>("runGallery") {
    group = "application"
    description = "Abre la galería de componentes para verificación visual."
    mainClass.set("com.zsilent.core.ui.gallery.ZGalleryKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.matching {
    it.name in setOf(
        "createDistributable",
        "prepareAppResources",
        "packageExe",
        "packageDistributionForCurrentOS",
        "packageMsi",
        "runDistributable"
    )
}.configureEach {
    dependsOn(prepareZSilentRuntimeResources)
}

tasks.matching { it.name == "packageMsi" }.configureEach {
    doLast {
        val msiFile = layout.buildDirectory
            .file("compose/binaries/main/msi/ZSilent Core-$releaseVersion.msi")
            .get()
            .asFile
        if (!msiFile.isFile) {
            throw GradleException("MSI was not generated at ${msiFile.absolutePath}")
        }

        val patchScript = project.file("scripts/patch-launch-after-install.ps1")
        if (!patchScript.isFile) {
            throw GradleException("Missing MSI patch script: ${patchScript.absolutePath}")
        }

        exec {
            workingDir(projectDir)
            commandLine(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", patchScript.absolutePath,
                "-MsiPath", msiFile.absolutePath
            )
            isIgnoreExitValue = false
        }
    }
}
