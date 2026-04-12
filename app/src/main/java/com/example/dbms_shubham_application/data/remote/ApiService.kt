package com.example.dbms_shubham_application.data.remote

import com.example.dbms_shubham_application.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<Map<String, String>>

    @POST("attendance/verify-wifi")
    suspend fun verifyWifi(@Body wifiRequest: WifiRequest): Response<WifiResponse>

    @POST("attendance/verify-qr")
    suspend fun verifyQr(@Body qrRequest: QrRequest): Response<Map<String, Any>>

    @Multipart
    @POST("attendance/verify-face")
    suspend fun verifyFace(
        @Part image: MultipartBody.Part,
        @Part("student_id") studentId: String
    ): Response<FaceResponse>

    @GET("student/attendance/{studentId}")
    suspend fun getAttendanceHistory(@Path("studentId") studentId: String): Response<List<AttendanceRecord>>
}
