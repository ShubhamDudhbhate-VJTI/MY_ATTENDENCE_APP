package com.example.dbms_shubham_application.data.model

import com.google.gson.annotations.SerializedName

data class StartSessionRequest(
    @SerializedName("faculty_id") val faculty_id: String,
    @SerializedName("subject_id") val subject_id: String,
    @SerializedName("classroom_id") val classroom_id: String,
    @SerializedName("duration_minutes") val duration_minutes: Int = 45
)

data class SessionResponse(
    @SerializedName("session_id") val session_id: String,
    @SerializedName("qr_token") val qr_token: String,
    @SerializedName("expires_at") val expires_at: String,
    @SerializedName("classroom_name") val classroom_name: String
)

data class AttendanceLog(
    @SerializedName("student_id") val student_id: String,
    @SerializedName("student_name") val student_name: String? = null,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("status") val status: String,
    @SerializedName("face_verified") val face_verified: Boolean = false
)

data class LiveAttendanceResponse(
    @SerializedName("session_id") val session_id: String,
    @SerializedName("total_count") val total_count: Int,
    @SerializedName("students") val students: List<AttendanceLog>
)

data class WifiRequest(
    @SerializedName("session_id") val session_id: String,
    @SerializedName("bssid") val bssid: String,
    @SerializedName("ssid") val ssid: String,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class WifiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("location_match") val location_match: Boolean = false
)

data class QrRequest(
    @SerializedName("session_id") val session_id: String,
    @SerializedName("token") val token: String
)

data class FaceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user_id") val user_id: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("message") val message: String? = null
)

data class AttendanceRecord(
    @SerializedName("subject_id") val subject_id: String,
    @SerializedName("subject_name") val subject_name: String = "",
    @SerializedName("session_id") val session_id: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("status") val status: String
)

data class SubjectAttendance(
    @SerializedName("subject_id") val subject_id: String,
    @SerializedName("subject_name") val subject_name: String,
    @SerializedName("total_classes") val total_classes: Int,
    @SerializedName("attended_classes") val attended_classes: Int,
    @SerializedName("percentage") val percentage: Double
)

data class SessionDetailsResponse(
    @SerializedName("session_id") val session_id: String,
    @SerializedName("subject_name") val subject_name: String,
    @SerializedName("start_time") val start_time: String,
    @SerializedName("total_students") val total_students: Int,
    @SerializedName("students") val students: List<SessionStudentDetail>
)

data class SessionStudentDetail(
    @SerializedName("student_id") val student_id: String,
    @SerializedName("student_name") val student_name: String,
    @SerializedName("marked_at") val marked_at: String,
    @SerializedName("status") val status: String
)

data class FacultySessionRecord(
    @SerializedName("session_id") val session_id: String,
    @SerializedName("subject_id") val subject_id: String,
    @SerializedName("subject_name") val subject_name: String = "",
    @SerializedName("classroom_id") val classroom_id: String,
    @SerializedName("start_time") val start_time: String = "",
    @SerializedName("expires_at") val expires_at: String?,
    @SerializedName("status") val status: String,
    @SerializedName("student_count") val student_count: Int
)

data class Classroom(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("wifi_ssid") val wifi_ssid: String? = null,
    @SerializedName("wifi_bssid") val wifi_bssid: String? = null
)

data class Subject(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String? = null,
    @SerializedName("branch") val branch: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("department_id") val department_id: String? = null
)

data class ActiveSession(
    @SerializedName("session_id") val session_id: String,
    @SerializedName("subject_id") val subject_id: String,
    @SerializedName("classroom_name") val classroom_name: String,
    @SerializedName("expires_at") val expires_at: String
)

data class SessionReportResponse(
    @SerializedName("session_id") val session_id: String,
    @SerializedName("total_present") val total_present: Int,
    @SerializedName("students") val students: List<StudentReport>? = emptyList(),
    @SerializedName("course_id") val course_id: String? = null
)

data class StudentReport(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("time") val time: String
)

data class ScheduleRecord(
    @SerializedName("id") val id: String? = null,
    @SerializedName("day") val day: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("subject_id") val subject_id: String? = null,
    @SerializedName("subject_code") val subject_code: String? = null,
    @SerializedName("branch") val branch: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("room") val room: String,
    @SerializedName("classroom_id") val classroom_id: String? = null,
    @SerializedName("time") val time: String,
    @SerializedName("is_official") val is_official: Boolean = false
)

data class UserProfile(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val full_name: String,
    @SerializedName("role") val role: String,
    @SerializedName("image_url") val image_url: String? = null,
    @SerializedName("profile_photo_url") val profile_photo_url: String? = null,
    @SerializedName("academic") val academic: Map<String, String?>? = emptyMap()
)

data class SyncScheduleResponse(
    @SerializedName("date") val date: String,
    @SerializedName("day") val day: String,
    @SerializedName("schedule") val schedule: List<ScheduleRecord>
)

data class NotificationRecord(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("is_read") val is_read: Boolean,
    @SerializedName("created_at") val created_at: String
)

data class LeaveRequestRecord(
    @SerializedName("id") val id: String,
    @SerializedName("student_id") val student_id: String,
    @SerializedName("type") val type: String, // OD, Medical, Other
    @SerializedName("start_date") val start_date: String,
    @SerializedName("end_date") val end_date: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("status") val status: String, // Pending, Approved, Rejected
    @SerializedName("created_at") val created_at: String
)

data class HeatmapPoint(
    @SerializedName("name") val name: String,
    @SerializedName("reg_no") val reg_no: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("verified") val verified: Boolean,
    @SerializedName("timestamp") val timestamp: String
)
