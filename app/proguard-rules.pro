# --- KOTLIN & GENERAL ---
-keepattributes Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable, *Annotation*, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# --- RETROFIT 2 ---
-keepattributes Signature, *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.example.dbms_shubham_application.data.remote.ApiService { *; }
-dontwarn retrofit2.**

# --- GSON & MODELS ---
-keep class com.google.gson.** { *; }
-keep class com.example.dbms_shubham_application.data.model.** { *; }
-keepclassmembers class com.example.dbms_shubham_application.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- OKHTTP ---
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
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

# --- ML Kit Barcode Scanning ---
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_barcode_scanning.** { *; }
-dontwarn com.google.mlkit.**

# --- CameraX ---
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# --- Google Play Services & Firebase ---
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Firebase Messaging ---
-keep class com.google.firebase.messaging.** { *; }
-keep public class com.example.dbms_shubham_application.service.MyFirebaseMessagingService { *; }
-keep public class com.example.dbms_shubham_application.service.NotificationReceiver { *; }
