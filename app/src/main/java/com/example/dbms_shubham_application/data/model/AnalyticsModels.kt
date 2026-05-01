package com.example.dbms_shubham_application.data.model

data class DepartmentAnalytics(
    val avg_attendance: String,
    val total_classes: Int,
    val defaulter_count: Int,
    val total_faculty: Int = 0,
    val total_students: Int = 0,
    val trends: List<TrendData>
)

data class TrendData(
    val month: String,
    val value: Int
)

data class FacultyAnalytics(
    val avg_attendance: String,
    val total_classes: Int,
    val defaulter_count: Int,
    val trends: List<TrendData>
)
