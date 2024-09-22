-keepclasseswithmembers class com.osmerion.android.database.** {
  native <methods>;
  public <init>(...);
}
-keepnames class com.osmerion.android.database.** { *; }
-keep public class com.osmerion.android.database.sqlite.SQLiteFunction { *; }
-keep public class com.osmerion.android.database.sqlite.SQLiteConnection { *; }
-keep public class com.osmerion.android.database.sqlite.SQLiteCustomFunction { *; }
-keep public class com.osmerion.android.database.sqlite.SQLiteCursor { *; }
-keep public class com.osmerion.android.database.sqlite.SQLiteDebug** { *; }
-keep public class com.osmerion.android.database.sqlite.SQLiteDatabase { *; }
-keep public class com.osmerion.android.database.sqlite.SQLiteOpenHelper { *; }
-keep public class com.osmerion.android.database.sqlite.SQLiteStatement { *; }
-keep public class com.osmerion.android.database.CursorWindow { *; }
-keepattributes Exceptions,InnerClasses
