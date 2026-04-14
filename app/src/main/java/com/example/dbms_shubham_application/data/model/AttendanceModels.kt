package com.example.dbms_shubham_application.data.model

data class StartSessionRequest(
    val faculty_id: String,
    val subject_id: String,
    val classroom_id: String,
    val duration_minutes: Int = 45
)

data class SessionResponse(
    val session_id: String,
    val qr_token: String,
    val expires_at: String,
    val classroom_name: String
)

data class AttendanceLog(
    val student_id: String,
    val student_name: String? = null,
    val timestamp: String,
    val status: String,
    val face_verified: Boolean = false
)

data class LiveAttendanceResponse(
    val session_id: String,
    val total_count: Int,
    val students: List<AttendanceLog>
)

data class WifiRequest(
    val session_id: String,
    val bssid: String,
    val ssid: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class WifiResponse(
    val success: Boolean,
    val message: String,
    val location_match: Boolean = false
)

data class QrRequest(
    val session_id: String,
    val token: String
)

data class FaceResponse(
    val success: Boolean,
    val message: String
)

data class AttendanceRecord(
    val subject_id: String,
    val session_id: String,
    val timestamp: String,
    val status: String
)

data class SessionDetailsResponse(
    val session_id: String,
    val subject_name: String,
    val start_time: String,
    val total_students: Int,
    val students: List<SessionStudentDetail>
)

data class SessionStudentDetail(
    val student_id: String,
    val student_name: String,
    val marked_at: String,
    val status: String
)

data class FacultySessionRecord(
    val session_id: String,
    val subject_id: String,
    val subject_name: String = "",
    val classroom_id: String,
    val start_time: String = "",
    val expires_at: String?,
    val status: String,
    val student_count: Int
)

data class Classroom(
    val id: String,
    val name: String,
    val wifi_ssid: String? = null,
    val wifi_bssid: String? = null
)

data class Subject(
    val id: String,
    val name: String,
    val code: String? = null,
    val branch: String? = null,
    val year: String? = null,
    val department_id: String? = null
)

data class ActiveSession(
    val session_id: String,
    val subject_id: String,
    val classroom_name: String,
    val expires_at: String
)

data class SessionReportResponse(
    val session_id: String,
    val total_present: Int,
    val students: List<StudentReport>,
    val course_id: String
)

data class StudentReport(
    val id: String,
    val name: String,
    val time: String
)

data class ScheduleRecord(
    val id: String? = null,
    val day: String,
    val subject: String,
    val subject_id: String? = null,
    val subject_code: String? = null,
    val branch: String? = null,
    val year: String? = null,
    val room: String,
    val classroom_id: String? = null,
    val time: String
)

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val full_name: String,
    val role: String,
    val image_url: String? = null,
    val academic: Map<String, String?>
)

data class NotificationRecord(
    val id: String,
    val title: String,
    val message: String,
    val is_read: Boolean,
    val created_at: String
)
