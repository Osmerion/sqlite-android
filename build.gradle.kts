/*
 * Copyright 2005-2012 The Android Open Source Project
 * Copyright 2017-2024 requery.io
 * Copyright 2024 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.osmerion.build.Download

plugins {
    alias(buildDeps.plugins.android.library)
    id("com.osmerion.maven-publish-conventions")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

android {
    buildToolsVersion = "34.0.0"
    ndkVersion = "26.1.10909125"

    compileSdk = 34

    namespace = "com.osmerion.android.sqlite"

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    lint {
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    testOptions {
        managedDevices {
            localDevices {
                register("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

tasks {
    val downloadSqlite = register<Download>("downloadSqlite") {
        src = "https://www.sqlite.org/2024/sqlite-amalgamation-3460100.zip"
        dest = layout.buildDirectory.file("tmp/sqlite.zip")

        checksum = "af6aae8d3eccc608857c63cf56efbadc70da48b5c719446b353ed88dded1e288"
        hashingAlgorithm = "SHA3-256"

        outputs.upToDateWhen { true }
    }

    val installSqlite = register<Copy>("installSqlite") {
        dependsOn(downloadSqlite)

        includeEmptyDirs = false

        from(zipTree(downloadSqlite.get().dest).matching {
            include("*/sqlite3.*")
            eachFile { path = name }
        })

        into("src/main/jni/sqlite")
    }

    preBuild {
        dependsOn(installSqlite)
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            /*
             * Unfortunately, the "afterEvaluate" is necessary because the Android plugin registers
             * the components too late.
             */
            afterEvaluate {
                from(components["release"])
            }

            pom {
                description = "A distribution of the latest SQLite versions for Android."
                packaging = "aar"
            }
        }
    }
}

dependencies {
    api(libs.androidx.core)
    api(libs.androidx.sqlite)

    testImplementation(buildDeps.junit)
    androidTestImplementation(buildDeps.androidx.test.core)
    androidTestImplementation(buildDeps.androidx.test.runner)
    androidTestImplementation(buildDeps.androidx.test.rules)
    androidTestImplementation(buildDeps.androidx.test.ext.junit)
}