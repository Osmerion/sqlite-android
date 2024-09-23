# Android SQLite support library

[![License](https://img.shields.io/badge/license-Apache%202.0-yellowgreen.svg?style=for-the-badge&label=License)](https://github.com/Osmerion/sqlite-android/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.osmerion.sqlite.android/sqlite-android.svg?style=for-the-badge&label=Maven%20Central)](https://maven-badges.herokuapp.com/maven-central/com.osmerion.sqlite.android/sqlite-android)
![Java](https://img.shields.io/badge/Java-17-green.svg?style=for-the-badge&color=b07219&logo=Java)
![Android SDK](https://img.shields.io/badge/Android%20SDK-21-green.svg?style=for-the-badge&color=34A853&logo=Java)

This project is a distribution of the latest versions of SQLite for Android. It implements the [androidx.sqlite](https://developer.android.com/jetpack/androidx/releases/sqlite)
API for broad compatibility with Android libraries. In many cases, this library can be used as a drop-in
replacement for the Android platform's built-in SQLite libraries.

This repository was forked from [requery/sqlite-android](https://github.com/requery/sqlite-android)
to streamline the build process, which allows us to support newer versions of SQLite faster, and to
provide stable artifacts reliably via Maven Central.


## Why?

Redistributing SQLite with Android apps has several advantages:

  - **Consistency**: All installations of the app use the same build of SQLite. This simplifies
    testing and debugging.
  - **Feature Availability**: The latest features of SQLite are available, regardless of which
    Android versions the app is targeting.
  - **Reliability**: The latest bug fixes and performance are available to all installations of
    the app.

The version of SQLite provided by the Android platform, usually lags behind several versions.
Examples of newer SQLite features include:

  - **[JSON1 extension](https://www.sqlite.org/json1.html)**
  - **[Common Table expressions](https://www.sqlite.org/lang_with.html)**
  - **[Indexes on expressions](https://www.sqlite.org/expridx.html)**
  - **[Full-Text Search 5](https://www.sqlite.org/fts5.html)**
  - **[Generated Columns](https://www.sqlite.org/gencol.html)**
  - **[DROP COLUMN support](https://www.sqlite.org/lang_altertable.html#altertabdropcol)**


## Usage

After [adding the library dependency](https://central.sonatype.com/artifact/com.osmerion.sqlite.android/sqlite-android),
the library can be used as a drop-in replacement for Android's built-in SQLite libraries by
replacing all `android.database.sqlite` classes with their `com.osmerion.android.database.sqlite`
counterparts.

If you expose `Cursor` instances across processes you should wrap the returned cursors in a
[CrossProcessCursorWrapper](http://developer.android.com/reference/android/database/CrossProcessCursorWrapper.html)
for performance reasons the cursors are not a cross process by default.

### Support library compatibility

This library implements the [androidx.sqlite](https://developer.android.com/jetpack/androidx/releases/sqlite)
API for broad compatibility with Android libraries. Use `OsmerionSQLiteOpenHelperFactory` to obtain
an implementation of `SupportSQLiteOpenHelper`. This can then be used with libraries like Room.


## CPU Architectures

The native library is built for the following CPU architectures:

  - `armeabi-v7a` ~1.2 MB
  - `arm64-v8a` ~1.7 MB
  - `x86` ~1.7 MB
  - `x86_64` ~1.8 MB

However, you may not want to include all binaries in your apk.
You can exclude certain variants by using `packagingOptions`:

```kotlin
android {
    packagingOptions {
        exclude("lib/armeabi-v7a/libsqlite3x.so")
        exclude("lib/arm64-v8a/libsqlite3x.so")
        exclude("lib/x86/libsqlite3x.so")
        exclude("lib/x86_64/libsqlite3x.so")
    }
}
```

The size of the artifacts with only the armeabi-v7a binary is **~1.2 MB**.
In general, you can use armeabi-v7a on the majority of Android devices including Intel Atom
which provides a native translation layer, however, performance under the translation layer
is worse than using the x86 binary.


## Acknowledgements & Changes

This repository was forked from [requery/sqlite-android](https://github.com/requery/sqlite-android)
which, in turn, is based on the AOSP code and the [Android SQLite bindings](https://www.sqlite.org/android/doc/trunk/www/index.wiki).
Compared to the original AOSP code, the following changes have been made:

  - **Faster read performance**: The original SQLite bindings filled the CursorWindow using its Java
    methods from native C++. This was because there is no access to the native CursorWindow native
    API from the NDK. Unfortunately, this slowed read performance significantly (roughly 2x worse vs
    the android database API) because of extra JNI roundtrips. This has been rewritten
    without the JNI to Java calls (so more like the original AOSP code) and also using a local memory
    CursorWindow.
  - Instead of replicating the entire API of `android.database.sqlite`, this project reuses the
    original classes when possible. This, in addition to use of the AndroidX SQLite API,
    significantly simplifies migration and/or use with existing code.
  - The test coverage has been expanded and improved.
  - Consumer ProGuard rules have been added.
  - Several deprecated classes and methods have been removed.
  - The native library is built with several extensions that are not enabled by default in the
    Android platform's SQLite library, including FTS3, FTS4, and JSON1.
  - The native library is built with [loadable extension support](https://www.sqlite.org/loadext.html).


## License

```
Copyright 2005-2012 The Android Open Source Project
Copyright 2017-2024 requery.io
Copyright 2024 Leon Linhart

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```