package com.example.dbms_shubham_application.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatIsoToDisplay(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return "---"
        return try {
            val date = isoFormat.parse(isoString.replace("Z", ""))
            date?.let { displayFormat.format(it) } ?: isoString.take(16).replace("T", " ")
        } catch (e: Exception) {
            isoString.take(16).replace("T", " ")
        }
    }

    fun formatTimeOnly(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return "--:--"
        return try {
            if (isoString.contains("T")) {
                isoString.substringAfter("T").take(5)
            } else {
                isoString.take(5)
            }
        } catch (e: Exception) {
            "--:--"
        }
    }

    fun formatDateOnly(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return "---"
        return try {
            if (isoString.contains("T")) {
                isoString.split("T")[0]
            } else {
                isoString.take(10)
            }
        } catch (e: Exception) {
            isoString.take(10)
        }
    }

    fun getCurrentDayName(): String {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    }

    fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val format = if (timeStr.contains("AM", true) || timeStr.contains("PM", true)) {
                "hh:mm a"
            } else {
                "HH:mm"
            }
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            val date = sdf.parse(timeStr.trim()) ?: return 0
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) {
            0
        }
    }

    fun getCurrentMinutesSinceMidnight(): Int {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }
}
