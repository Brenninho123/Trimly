import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose") version "1.7.1"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.json:json:20240303")
}

compose.desktop {
    application {
        mainClass = "com.brenninho.trimly.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Trimly"
            packageVersion = "1.0.0"
            description = "A free video editor"
            vendor = "Brenninho"
            modules("java.instrument", "jdk.unsupported")
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            windows {
                menuGroup = "Trimly"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "8f6c3a52-5d1e-4b7a-9c0e-2f4a6b1d9e33"
                iconFile.set(project.file("src/main/resources/trimly.ico"))
            }

            macOS {
                bundleID = "com.brenninho.trimly"
                dockName = "Trimly"
                val icnsFile = project.file("icons/trimly.icns")
                if (icnsFile.isFile) {
                    iconFile.set(icnsFile)
                }
            }

            linux {
                shortcut = true
                packageName = "trimly"
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}
