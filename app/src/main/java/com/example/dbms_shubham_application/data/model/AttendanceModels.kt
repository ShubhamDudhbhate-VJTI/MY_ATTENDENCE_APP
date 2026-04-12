package com.example.dbms_shubham_application.data.model

data class WifiRequest(
    val bssid: String,
    val ssid: String
)

data class WifiResponse(
    val success: Boolean,
    val message: String,
    val classroomName: String? = null
)

data class QrRequest(
    val token: String,
    val sessionId: String
)

data class FaceResponse(
    val success: Boolean,
    val confidence: Float,
    val message: String
)

data class AttendanceRecord(
    val studentId: String,
    val sessionId: String,
    val timestamp: Long,
    val status: String
)
