package com.example.dbms_shubham_application.network

import com.example.dbms_shubham_application.data.remote.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.os.Build
import android.util.Log

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    
    // CURRENT PC IP (Change this if ipconfig changes)
    private const val PC_IP = "172.23.148.244"

    private fun getBaseUrl(): String {
        // Using localhost:8000 with 'adb reverse' for the most stable connection
        return "http://localhost:8000/"
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request()
            try {
                chain.proceed(request)
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Network error for ${request.url}: ${e.message}", e)
                throw e
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS) 
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val apiService: ApiService by lazy {
        val url = getBaseUrl()
        Log.d(TAG, "Initializing Retrofit with URL: $url")
        Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ApiService::class.java)
    }

    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val product = Build.PRODUCT
        val brand = Build.BRAND
        val device = Build.DEVICE
        
        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || brand.startsWith("generic") && device.startsWith("generic")
                || product == "google_sdk"
                || product == "sdk_gphone_x86"
                || product == "vbox86p"
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
    }
}
