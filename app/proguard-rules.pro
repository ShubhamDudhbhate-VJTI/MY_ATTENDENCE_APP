# Retrofit 2
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Your App Data Models (Crucial to prevent renaming fields)
-keep class com.example.dbms_shubham_application.data.model.** { *; }
-keepclassmembers class com.example.dbms_shubham_application.data.model.** { *; }

# Keep Network/Remote Interfaces
-keep class com.example.dbms_shubham_application.data.remote.** { *; }
-keep interface com.example.dbms_shubham_application.data.remote.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Kotlin Serialization (if used) or Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$Companion {
    private static final long serialVersionUID;
}
