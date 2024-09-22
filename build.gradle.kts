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

        from(zipTree(downloadSqlite.get().dest).matching {
            include("*/sqlite3.*")
            eachFile { path = name }
        })

        into("src/main/jni/sqlite")
    }

    preBuild {
        dependsOn(installSqlite)
    }

    register<Javadoc>("javadoc") {
        source(android.sourceSets["main"].java.srcDirs)

        classpath += project.files(android.bootClasspath.joinToString(File.pathSeparator))

        exclude("**/R.html", "**/R.*.html", "**/index.html")

        isFailOnError = false
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