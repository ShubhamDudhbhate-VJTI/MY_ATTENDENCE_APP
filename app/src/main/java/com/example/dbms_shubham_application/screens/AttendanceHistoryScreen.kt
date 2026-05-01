package com.example.dbms_shubham_application.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.components.ModernAttendanceCard
import com.example.dbms_shubham_application.utils.PredictiveAttendanceUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val userId = remember { sessionManager.getUserId() ?: "" }
    
    val colorScheme = MaterialTheme.colorScheme
    
    var attendanceRecords by remember { mutableStateOf<List<com.example.dbms_shubham_application.data.model.AttendanceRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    val fetchAttendance = {
        scope.launch {
            isRefreshing = true
            try {
                val response = RetrofitClient.apiService.getAttendanceHistory(userId)
                if (response.isSuccessful) {
                    attendanceRecords = (response.body() ?: emptyList()).sortedByDescending { it.timestamp }
                }
            } catch (e: Exception) {
                Log.e("AttendanceHistory", "Error: ${e.message}")
            } finally {
                isRefreshing = false
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            fetchAttendance()
        }
    }
    
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Academic Audit", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Your attendance journey", color = colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                            .background(colorScheme.surface, CircleShape)
                            .border(1.dp, colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { fetchAttendance() },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .background(colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { fetchAttendance() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (isLoading && attendanceRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            } else if (attendanceRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, null, tint = colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(120.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No attendance records found", color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Stats Overview
                    item {
                        val total = attendanceRecords.size
                        val present = attendanceRecords.count { it.status.lowercase() == "present" }
                        val absent = total - present
                        val percentage = if (total > 0) (present.toFloat() / total) else 0f
                        
                        Column {
                            ModernHistoryStatsCard(percentage, present, absent, total)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            PredictiveAlertCard(present, total)
                        }
                    }
                    
                    item {
                        Text(
                            text = "Attendance Records",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Attendance Records
                    items(attendanceRecords) { record ->
                        ModernAttendanceCard(record = record)
                    }
                    
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun ModernHistoryStatsCard(percentage: Float, present: Int, absent: Int, total: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val percentText = "%.1f%%".format(percentage * 100)
    val statusColor = if (percentage >= 0.75f) colorScheme.primary else colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(28.dp), spotColor = colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Attendance Velocity", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Text(percentText, fontSize = 40.sp, fontWeight = FontWeight.Black, color = colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(listOf(statusColor.copy(alpha = 0.1f), statusColor.copy(alpha = 0.05f))),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (percentage >= 0.75f) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = statusColor,
                trackColor = colorScheme.outline.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ModernMiniStatItem("Present", present.toString(), colorScheme.primary)
                VerticalDivider(modifier = Modifier.height(30.dp), color = colorScheme.outline.copy(alpha = 0.2f))
                ModernMiniStatItem("Absent", absent.toString(), colorScheme.error)
                VerticalDivider(modifier = Modifier.height(30.dp), color = colorScheme.outline.copy(alpha = 0.2f))
                ModernMiniStatItem("Total", total.toString(), colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ModernMiniStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun PredictiveAlertCard(present: Int, total: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val target = 0.75f
    val currentPercentage = if (total > 0) present.toFloat() / total else 0f
    
    val isSafe = currentPercentage >= target
    val statusColor = if (isSafe) colorScheme.primary else colorScheme.error
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = statusColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                if (isSafe) {
                    val affordableAbsences = PredictiveAttendanceUtils.calculateAffordableAbsences(present, total, target)
                    Text(
                        "Safely Above Target",
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        fontSize = 16.sp
                    )
                    Text(
                        if (affordableAbsences > 0) 
                            "You can afford to miss $affordableAbsences more classes." 
                        else 
                            "Maintaining target! Don't miss next class.",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                } else {
                    val requiredClasses = PredictiveAttendanceUtils.calculateRequiredClasses(present, total, target)
                    Text(
                        "Attendance Alert",
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        fontSize = 16.sp
                    )
                    Text(
                        "Attend next $requiredClasses classes consecutively to reach 75%.",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
