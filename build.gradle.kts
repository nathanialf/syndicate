// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // AGP 9 compiles Kotlin itself ("built-in Kotlin") with its bundled KGP; pin the compiler the project uses
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}