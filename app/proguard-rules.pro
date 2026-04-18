# --- KOTLIN & GENERAL ---
-keepattributes Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable, *Annotation*, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# --- RETROFIT 2 ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

# Service interfaces must be kept
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# --- GSON ---
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.internal.** { *; }

# --- DATA MODELS ---
-keep class com.example.dbms_shubham_application.data.model.** { *; }
-keepclassmembers class com.example.dbms_shubham_application.data.model.** { *; }

# --- OKHTTP ---
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# --- FIX FOR "Class cannot be cast to..." ---
-keep class retrofit2.Response { *; }
-keep interface com.example.dbms_shubham_application.data.remote.ApiService { *; }

# Specific preservation for Generic Types
-keep class java.lang.reflect.Type
-keep class java.lang.reflect.ParameterizedType
-keep class java.lang.reflect.GenericArrayType
-keep class java.lang.reflect.TypeVariable
-keep class java.lang.reflect.WildcardType

# Keep everything used in Response<?>
-keep class * {
  @retrofit2.http.* <methods>;
}

# --- LOGGING INTERCEPTOR ---
-keep class okhttp3.logging.HttpLoggingInterceptor { *; }
-keep enum okhttp3.logging.HttpLoggingInterceptor$Level { *; }
