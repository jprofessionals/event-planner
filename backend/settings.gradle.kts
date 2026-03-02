pluginManagement {
    val quarkusPluginVersion: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version quarkusPluginVersion
        id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    }
}

rootProject.name = "meet-backend"
