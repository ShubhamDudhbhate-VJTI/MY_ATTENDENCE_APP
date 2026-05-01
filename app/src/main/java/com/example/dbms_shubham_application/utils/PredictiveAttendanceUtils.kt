package com.example.dbms_shubham_application.utils

import kotlin.math.ceil

object PredictiveAttendanceUtils {
    
    /**
     * Calculates how many more consecutive classes a student needs to attend 
     * to reach the target attendance percentage.
     * 
     * Formula: 
     * (Present + X) / (Total + X) >= Target
     * Present + X >= Target * (Total + X)
     * Present + X >= Target * Total + Target * X
     * X - Target * X >= Target * Total - Present
     * X(1 - Target) >= Target * Total - Present
     * X >= (Target * Total - Present) / (1 - Target)
     */
    fun calculateRequiredClasses(present: Int, total: Int, targetPercentage: Float = 0.75f): Int {
        if (total == 0) return ceil(targetPercentage * 1).toInt() // Assume at least 1 class if total is 0
        
        val currentPercentage = present.toFloat() / total
        if (currentPercentage >= targetPercentage) return 0
        
        val required = (targetPercentage * total - present) / (1 - targetPercentage)
        return ceil(required).toInt()
    }

    /**
     * Calculates how many classes a student can afford to miss 
     * before falling below the target attendance percentage.
     * 
     * Formula:
     * Present / (Total + Y) >= Target
     * Present >= Target * (Total + Y)
     * Present >= Target * Total + Target * Y
     * Present - Target * Total >= Target * Y
     * (Present - Target * Total) / Target >= Y
     */
    fun calculateAffordableAbsences(present: Int, total: Int, targetPercentage: Float = 0.75f): Int {
        if (total == 0) return 0
        
        val currentPercentage = present.toFloat() / total
        if (currentPercentage < targetPercentage) return 0
        
        val affordable = (present - targetPercentage * total) / targetPercentage
        return affordable.toInt()
    }
}
