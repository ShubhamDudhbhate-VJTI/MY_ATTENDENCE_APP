# Retrofit 2
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.internal.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# CRITICAL: Keep all Data Models and their field names exactly as they are
# This prevents "ClassCastException" during JSON parsing
-keep class com.example.dbms_shubham_application.data.model.** { *; }
-keepclassmembers class com.example.dbms_shubham_application.data.model.** { *; }
-keepnames class com.example.dbms_shubham_application.data.model.** { *; }

# Keep Network/Remote Interfaces
-keep class com.example.dbms_shubham_application.data.remote.** { *; }
-keep interface com.example.dbms_shubham_application.data.remote.** { *; }

# Kotlin Coroutines and Serialization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$Companion {
    private static final long serialVersionUID;
}
