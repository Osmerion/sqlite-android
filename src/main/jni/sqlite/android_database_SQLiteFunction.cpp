/*
 * Copyright 2005-2012 The Android Open Source Project
 * Copyright 2017-2024 requery.io
 * Copyright 2024-2025 Leon Linhart
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

#define LOG_TAG "SQLiteFunction"

#include <jni.h>
#include <sys/mman.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>

#include "sqlite3.h"
#include "JNIHelp.h"
#include "ALog-priv.h"
#include "android_database_SQLiteCommon.h"

namespace android {

/* Returns the sqlite3_value for the given arg of the given function.
 * If 0 is returned, an exception has been thrown to report the reason. */
static sqlite3_value *tovalue(JNIEnv *env, jlong argsPtr, jint arg) {
    if (arg < 0) {
        throw_sqlite3_exception(env, "Invalid arg index");
        return 0;
    }
    if (!argsPtr) {
        throw_sqlite3_exception(env, "Invalid argsPtr");
        return 0;
    }

    auto **args = reinterpret_cast<sqlite3_value**>(argsPtr);
    return args[arg];
}

static sqlite3_context *tocontext(JNIEnv *env, jlong contextPtr) {
    if (!contextPtr) {
        throw_sqlite3_exception(env, "Invalid contextPtr");
        return 0;
    }

    return reinterpret_cast<sqlite3_context*>(contextPtr);
}

/*
 * Getters
 */

static jbyteArray nativeGetArgBlob(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong argsPtr,
        jint arg) {
    int length;
    jbyteArray byteArray;
    const void *blob;

    sqlite3_value *value = tovalue(env, argsPtr, arg);
    if (!value) return nullptr;

    blob = sqlite3_value_blob(value);
    if (!blob) return nullptr;

    length = sqlite3_value_bytes(value);
    byteArray = env->NewByteArray(length);
    if (!byteArray) {
        env->ExceptionClear();
        throw_sqlite3_exception(env, "Native could not create new byte[]");
        return nullptr;
    }

    env->SetByteArrayRegion(byteArray, 0, length, static_cast<const jbyte*>(blob));
    return byteArray;
}

static jstring nativeGetArgString(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong argsPtr,
        jint arg) {
    sqlite3_value *value = tovalue(env, argsPtr, arg);
    if (!value) return nullptr;

    const auto* chars = static_cast<const jchar*>(sqlite3_value_text16(value));
    if (!chars) return nullptr;

    size_t len = sqlite3_value_bytes16(value) / sizeof(jchar);
    jstring str = env->NewString(chars, len);
    if (!str) {
        env->ExceptionClear();
        throw_sqlite3_exception(env, "Native could not allocate string");
        return nullptr;
    }

    return str;
}

static jlong nativeGetArgLong(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong argsPtr,
        jint arg) {
    sqlite3_value *value = tovalue(env, argsPtr, arg);
    return value ? sqlite3_value_int64(value) : 0;
}

static jdouble nativeGetArgDouble(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong argsPtr,
        jint arg) {
    sqlite3_value *value = tovalue(env, argsPtr, arg);
    return value ? sqlite3_value_double(value) : 0;
}

static jint nativeGetArgInt(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong argsPtr,
        jint arg) {
    sqlite3_value *value = tovalue(env, argsPtr, arg);
    return value ? sqlite3_value_int(value) : 0;
}

/*
 * Setters
 */

static void nativeSetResultBlob(JNIEnv* env, [[maybe_unused]] jclass clazz,
        jlong contextPtr, jbyteArray result) {
    sqlite3_context *context = tocontext(env, contextPtr);
    if (!context) return;
    if (result == nullptr) {
        sqlite3_result_null(context);
        return;
    }

    jsize len = env->GetArrayLength(result);
    void *bytes = env->GetPrimitiveArrayCritical(result, nullptr);
    if (!bytes) {
        env->ExceptionClear();
        throw_sqlite3_exception(env, "Out of memory accepting blob");
        return;
    }

    sqlite3_result_blob(context, bytes, len, SQLITE_TRANSIENT);
    env->ReleasePrimitiveArrayCritical(result, bytes, JNI_ABORT);
}

static void nativeSetResultString(JNIEnv* env, [[maybe_unused]] jclass clazz,
        jlong contextPtr, jstring result) {
    sqlite3_context *context = tocontext(env, contextPtr);
    if (result == nullptr) {
        sqlite3_result_null(context);
        return;
    }

    const char* chars = env->GetStringUTFChars(result, nullptr);
    if (!chars) {
        ALOGE("result value can't be transferred to UTFChars");
        sqlite3_result_error_nomem(context);
        return;
    }

    sqlite3_result_text(context, chars, -1, SQLITE_TRANSIENT);
    env->ReleaseStringUTFChars(result, chars);
}

static void nativeSetResultLong(JNIEnv* env, [[maybe_unused]] jclass clazz,
        jlong contextPtr, jlong result) {
    sqlite3_context *context = tocontext(env, contextPtr);
    if (context) sqlite3_result_int64(context, result);
}

static void nativeSetResultDouble(JNIEnv* env, [[maybe_unused]] jclass clazz,
        jlong contextPtr, jdouble result) {
    sqlite3_context *context = tocontext(env, contextPtr);
    if (context) sqlite3_result_double(context, result);
}

static void nativeSetResultInt(JNIEnv* env, [[maybe_unused]] jclass clazz,
        jlong contextPtr, jint result) {
    sqlite3_context *context = tocontext(env, contextPtr);
    if (context) sqlite3_result_int(context, result);
}

static void nativeSetResultError(JNIEnv* env, [[maybe_unused]] jclass clazz,
        jlong contextPtr, jstring error) {
    sqlite3_context *context = tocontext(env, contextPtr);
    if (error == nullptr) {
        sqlite3_result_null(context);
        return;
    }

    const char* chars = env->GetStringUTFChars(error, nullptr);
    if (!chars) {
        ALOGE("result value can't be transferred to UTFChars");
        sqlite3_result_error_nomem(context);
        return;
    }

    sqlite3_result_error(context, chars, -1);
    env->ReleaseStringUTFChars(error, chars);
}

static void nativeSetResultNull(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong contextPtr) {
    sqlite3_context *context = tocontext(env, contextPtr);
    if (context) sqlite3_result_null(context);
}


static const JNINativeMethod sMethods[] =
{
    /* name, signature, funcPtr */
    { "nativeGetArgBlob", "(JI)[B",
            (void*)nativeGetArgBlob },
    { "nativeGetArgString", "(JI)Ljava/lang/String;",
            (void*)nativeGetArgString },
    { "nativeGetArgLong", "(JI)J",
            (void*)nativeGetArgLong },
    { "nativeGetArgDouble", "(JI)D",
            (void*)nativeGetArgDouble },
    { "nativeGetArgInt", "(JI)I",
            (void*)nativeGetArgInt },

    { "nativeSetResultBlob", "(J[B)V",
            (void*)nativeSetResultBlob },
    { "nativeSetResultString", "(JLjava/lang/String;)V",
            (void*)nativeSetResultString },
    { "nativeSetResultLong", "(JJ)V",
            (void*)nativeSetResultLong },
    { "nativeSetResultDouble", "(JD)V",
            (void*)nativeSetResultDouble },
    { "nativeSetResultInt", "(JI)V",
            (void*)nativeSetResultInt },
    { "nativeSetResultError", "(JLjava/lang/String;)V",
            (void*)nativeSetResultError },
    { "nativeSetResultNull", "(J)V",
            (void*)nativeSetResultNull },
};

int register_android_database_SQLiteFunction(JNIEnv* env)
{
    return jniRegisterNativeMethods(env,
        "com/osmerion/android/database/sqlite/SQLiteFunction", sMethods, NELEM(sMethods));
}

} // namespace android
