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
package com.osmerion.android.database.sqlite;

import android.content.Context;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import com.osmerion.android.database.DatabaseErrorHandler;

import java.util.Collections;

/**
 * Implements {@link SupportSQLiteOpenHelper.Factory} using the SQLite implementation shipped in
 * this library.
 */
@SuppressWarnings("unused")
public final class RequerySQLiteOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {
    private final Iterable<ConfigurationOptions> configurationOptions;

    @SuppressWarnings("WeakerAccess")
    public RequerySQLiteOpenHelperFactory(Iterable<ConfigurationOptions> configurationOptions) {
        this.configurationOptions = configurationOptions;
    }

    public RequerySQLiteOpenHelperFactory() {
        this(Collections.<ConfigurationOptions>emptyList());
    }

    @Override
    public SupportSQLiteOpenHelper create(SupportSQLiteOpenHelper.Configuration config) {
        return new CallbackSQLiteOpenHelper(config.context, config.name, config.callback, configurationOptions);
    }

    private static final class CallbackSQLiteOpenHelper extends SQLiteOpenHelper {

        private final SupportSQLiteOpenHelper.Callback callback;
        private final Iterable<ConfigurationOptions> configurationOptions;

        CallbackSQLiteOpenHelper(Context context, String name, SupportSQLiteOpenHelper.Callback cb, Iterable<ConfigurationOptions> ops) {
            super(context, name, null, cb.version, new CallbackDatabaseErrorHandler(cb));
            this.callback = cb;
            this.configurationOptions = ops;
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            callback.onConfigure(db);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            callback.onCreate(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            callback.onUpgrade(db, oldVersion, newVersion);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            callback.onDowngrade(db, oldVersion, newVersion);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            callback.onOpen(db);
        }

        @Override protected SQLiteDatabaseConfiguration createConfiguration(String path, int openFlags) {
            SQLiteDatabaseConfiguration config = super.createConfiguration(path, openFlags);

            for (ConfigurationOptions option : configurationOptions) {
                config = option.apply(config);
            }

            return config;
        }
    }

    private static final class CallbackDatabaseErrorHandler implements DatabaseErrorHandler {

        private final SupportSQLiteOpenHelper.Callback callback;

        CallbackDatabaseErrorHandler(SupportSQLiteOpenHelper.Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onCorruption(SQLiteDatabase db) {
            callback.onCorruption(db);
        }
    }

    public interface ConfigurationOptions {
        SQLiteDatabaseConfiguration apply(SQLiteDatabaseConfiguration configuration);
    }
}
