import de.undercouch.gradle.tasks.download.Download

plugins {
    id("com.android.library")
    id("com.osmerion.maven-publish-conventions")
    id("de.undercouch.download")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

android {
    buildToolsVersion = "34.0.0"
    ndkVersion = "26.1.10909125"

    compileSdk = 34

    namespace = "io.requery.android.sqlite"

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

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

tasks {
    val downloadSqlite = register<Download>("downloadSqlite") {
        src("https://www.sqlite.org/2024/sqlite-amalgamation-3450000.zip")
        dest("src/main/jni/sqlite.zip")
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
    api("androidx.core:core:1.12.0")
    api("androidx.sqlite:sqlite:2.4.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}