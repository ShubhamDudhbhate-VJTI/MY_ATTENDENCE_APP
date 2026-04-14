package com.example.dbms_shubham_application.data.remote

import com.example.dbms_shubham_application.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("sessions/start")
    suspend fun startSession(@Body request: StartSessionRequest): Response<SessionResponse>

    @POST("auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<Map<String, Any>>

    @POST("auth/signup")
    suspend fun signup(@Body userData: Map<String, String>): Response<Map<String, Any>>

    @POST("attendance/verify-wifi")
    suspend fun verifyWifi(@Body wifiRequest: WifiRequest): Response<WifiResponse>

    @POST("attendance/verify-qr")
    suspend fun verifyQr(@Body request: Map<String, String>): Response<Map<String, Any>>

    @Multipart
    @POST("attendance/verify-face")
    suspend fun verifyFace(
        @Part image: MultipartBody.Part?,
        @Part("student_id") studentId: okhttp3.RequestBody,
        @Part("session_id") sessionId: okhttp3.RequestBody
    ): Response<FaceResponse>

    @GET("sessions/{session_id}/attendance")
    suspend fun getLiveAttendance(
        @Path("session_id") sessionId: String
    ): Response<LiveAttendanceResponse>

    @GET("student/attendance/{studentId}")
    suspend fun getAttendanceHistory(@Path("studentId") studentId: String): Response<List<AttendanceRecord>>

    @GET("faculty/sessions/{facultyId}")
    suspend fun getFacultySessions(
        @Path("facultyId") facultyId: String,
        @Query("subject_id") subjectId: String? = null,
        @Query("classroom_id") classroomId: String? = null,
        @Query("date") date: String? = null
    ): Response<List<FacultySessionRecord>>

    @GET("sessions/{session_id}/details")
    suspend fun getSessionDetails(
        @Path("session_id") sessionId: String
    ): Response<SessionDetailsResponse>

    @GET("classrooms")
    suspend fun getClassrooms(): Response<List<Classroom>>

    @GET("subjects")
    suspend fun getSubjects(): Response<List<Subject>>

    @GET("faculty/subjects/{facultyId}")
    suspend fun getFacultySubjects(@Path("facultyId") facultyId: String): Response<List<Subject>>

    @GET("sessions/active")
    suspend fun getActiveSessions(): Response<List<ActiveSession>>

    @POST("sessions/stop/{session_id}")
    suspend fun stopSession(@Path("session_id") sessionId: String): Response<SessionReportResponse>

    @GET("faculty/schedule/{facultyId}")
    suspend fun getFacultySchedule(
        @Path("facultyId") facultyId: String,
        @Query("day") day: String? = null
    ): Response<List<ScheduleRecord>>

    @POST("faculty/schedule/{facultyId}")
    suspend fun addScheduleRecord(
        @Path("facultyId") facultyId: String,
        @Body record: ScheduleRecord
    ): Response<ScheduleRecord>

    @DELETE("faculty/schedule/{recordId}")
    suspend fun deleteScheduleRecord(
        @Path("recordId") recordId: String
    ): Response<Map<String, Any>>

    @PUT("faculty/schedule/{recordId}")
    suspend fun updateScheduleRecord(
        @Path("recordId") recordId: String,
        @Body record: ScheduleRecord
    ): Response<ScheduleRecord>

    @GET("student/schedule/{studentId}")
    suspend fun getStudentSchedule(
        @Path("studentId") studentId: String,
        @Query("day") day: String? = null
    ): Response<List<ScheduleRecord>>

    @GET("notifications/{userId}")
    suspend fun getNotifications(@Path("userId") userId: String): Response<List<NotificationRecord>>

    @GET("auth/me/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): Response<UserProfile>

    @POST("notifications/read/{notificationId}")
    suspend fun markNotificationAsRead(@Path("notificationId") notificationId: String): Response<Map<String, Any>>
}
