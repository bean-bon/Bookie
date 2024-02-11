import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
}

group = "ind.beanie"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jogamp.org/deployment/maven")
}

dependencies {
    val koinVersion = "3.6.0-wasm-alpha2"
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains:markdown:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("io.insert-koin:koin-compose:$koinVersion")
    implementation("org.jogamp.gluegen:gluegen-rt-main:2.5.0")
    implementation("org.jogamp.jogl:jogl-all-main:2.5.0")
    api("io.github.kevinnzou:compose-webview-multiplatform:1.8.6")
    // Testing dependencies.
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {

        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "BookieEditor"
            packageVersion = "1.0.0"
        }

        // Configuration for WebView library

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }

        buildTypes.release.proguard {
            configurationFiles.from("compose-desktop.pro")
        }

    }
}
