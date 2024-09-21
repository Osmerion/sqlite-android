pluginManagement {
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    }

    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

rootProject.name = "sqlite-android"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        register("buildDeps") {
            from(files("./gradle/build.versions.toml"))
        }
    }
}

include("sqlite-android")