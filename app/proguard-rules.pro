# 1. DASAR ANDROID & R8
-dontwarn android.**
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# 2. HILT & DAGGER (DI)
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class dagger.** { *; }
-dontwarn dagger.**

# 3. SQLCIPHER & ROOM (DATABASE ENKRIPSI)
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# 4. RETROFIT, OKHTTP, GSON/MOSHI (NETWORK & PARSING)
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep class com.google.gson.** { *; }

# 5. DATA MODELS (N8N / API)
-keep class id.yansproject.app.data.remote.dto.** { *; }
-keep class id.yansproject.app.domain.model.** { *; }

# 6. FIRM OBFUSCATION RULES FOR FIREBASE & DATABASE ENTITIES
-keep class com.yansproject.app.data.** { *; }
-keepnames class com.yansproject.app.data.** { *; }
-keep class com.google.firebase.** { *; }
