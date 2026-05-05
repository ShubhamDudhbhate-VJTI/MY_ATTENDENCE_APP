package com.example.dbms_shubham_application.data.remote

import com.example.dbms_shubham_application.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("sessions/start")
    suspend fun startSession(@Body request: StartSessionRequest): Response<SessionResponse>

    @POST("auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<LoginResponse>

    @POST("auth/signup")
    suspend fun signup(@Body userData: Map<String, String>): Response<LoginResponse>

    @POST("attendance/verify-wifi")
    suspend fun verifyWifi(@Body wifiRequest: WifiRequest): Response<WifiResponse>

    @POST("attendance/verify-qr")
    suspend fun verifyQr(@Body request: Map<String, String>): Response<Map<String, Any>>

    @Multipart
    @POST("attendance/verify-face")
    suspend fun verifyFace(
        @Part image: MultipartBody.Part?,
        @Part("student_id") studentId: okhttp3.RequestBody,
        @Part("session_id") sessionId: okhttp3.RequestBody,
        @Part("latitude") latitude: okhttp3.RequestBody?,
        @Part("longitude") longitude: okhttp3.RequestBody?
    ): Response<FaceResponse>

    @GET("sessions/{session_id}/attendance")
    suspend fun getLiveAttendance(
        @Path("session_id") sessionId: String
    ): Response<LiveAttendanceResponse>

    @GET("student/attendance/{studentId}")
    suspend fun getAttendanceHistory(@Path("studentId") studentId: String): Response<List<AttendanceRecord>>

    @GET("student/attendance/{studentId}/subjects")
    suspend fun getSubjectAttendance(@Path("studentId") studentId: String): Response<List<SubjectAttendance>>

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

    @POST("faculty/schedule/{facultyId}/sync-official")
    suspend fun syncOfficialSchedule(
        @Path("facultyId") facultyId: String,
        @Query("day") day: String? = null
    ): Response<SyncScheduleResponse>

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

    @POST("notifications/delete/{notificationId}")
    suspend fun deleteNotification(@Path("notificationId") notificationId: String): Response<Map<String, Any>>

    @POST("notifications/clear/{userId}")
    suspend fun clearAllNotifications(@Path("userId") userId: String): Response<Map<String, Any>>

    @DELETE("sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String): Response<Map<String, Any>>

    @POST("sessions/clear/{facultyId}")
    suspend fun clearAllSessions(@Path("facultyId") facultyId: String): Response<Map<String, Any>>

    @POST("attendance/manual")
    suspend fun addManualAttendance(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("reports/excel/{sessionId}")
    @Streaming
    suspend fun downloadReportExcel(@Path("sessionId") sessionId: String): Response<okhttp3.ResponseBody>

    @GET("reports/pdf/{sessionId}")
    @Streaming
    suspend fun downloadReportPdf(
        @Path("sessionId") sessionId: String,
        @Query("student_id") studentId: String? = null
    ): Response<okhttp3.ResponseBody>

    @GET("reports/bulk-pdf")
    @Streaming
    suspend fun downloadBulkReportPdf(
        @Query("faculty_id") facultyId: String,
        @Query("branch") branch: String? = "All",
        @Query("year") year: String? = "All",
        @Query("subject_id") subjectId: String? = "All",
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("student_id") studentId: String? = null
    ): Response<okhttp3.ResponseBody>

    @GET("reports/hod-master-pdf")
    @Streaming
    suspend fun downloadHodMasterPdf(
        @Query("department_id") departmentId: String,
        @Query("faculty_id") facultyId: String? = "All",
        @Query("branch") branch: String? = "All",
        @Query("year") year: String? = "All",
        @Query("subject_id") subjectId: String? = "All",
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("student_id") studentId: String? = null
    ): Response<okhttp3.ResponseBody>

    @GET("reports/hod-master-excel")
    @Streaming
    suspend fun downloadHodMasterExcel(
        @Query("department_id") departmentId: String,
        @Query("faculty_id") facultyId: String? = "All",
        @Query("branch") branch: String? = "All",
        @Query("year") year: String? = "All",
        @Query("subject_id") subjectId: String? = "All",
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("student_id") studentId: String? = null
    ): Response<okhttp3.ResponseBody>

    @GET("reports/summary")
    suspend fun getReportsSummary(
        @Query("faculty_id") facultyId: String? = null,
        @Query("department_id") departmentId: String? = null,
        @Query("branch") branch: String? = "All",
        @Query("year") year: String? = "All",
        @Query("subject_id") subjectId: String? = "All",
        @Query("student_id") studentId: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<Map<String, Int>>

    @GET("faculty/all")
    suspend fun getAllFaculty(): Response<List<UserProfile>>

    @POST("notifications/send")
    suspend fun sendNotification(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("auth/update-fcm")
    suspend fun updateFcmToken(@Body data: Map<String, String>): Response<Map<String, Any>>

    @GET("analytics/faculty/{facultyId}")
    suspend fun getFacultyAnalytics(
        @Path("facultyId") facultyId: String
    ): Response<FacultyAnalytics>

    @GET("debug/check-setup")
    suspend fun checkSetup(): Response<Map<String, Any>>

    @GET("analytics/department/{deptId}")
    suspend fun getDepartmentAnalytics(
        @Path("deptId") deptId: String
    ): Response<DepartmentAnalytics>

    @GET("analytics/department/{deptId}/export")
    suspend fun exportDepartmentExcel(
        @Path("deptId") deptId: String
    ): Response<ResponseBody>

    @Multipart
    @POST("users/{userId}/profile-photo")
    suspend fun uploadProfilePhoto(
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<Map<String, Any>>

    @PUT("users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body data: Map<String, String>
    ): Response<Map<String, Any>>

    // Leave Management
    @POST("leaves/request")
    suspend fun submitLeaveRequest(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("leaves/history/{userId}")
    suspend fun getLeaveHistory(@Path("userId") userId: String): Response<List<LeaveRequestRecord>>
}
