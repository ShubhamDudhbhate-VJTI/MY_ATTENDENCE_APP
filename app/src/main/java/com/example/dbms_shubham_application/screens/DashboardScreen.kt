package com.example.dbms_shubham_application.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.AttendanceRecord
import com.example.dbms_shubham_application.data.model.FacultySessionRecord
import com.example.dbms_shubham_application.data.model.ScheduleRecord
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

// --- MODERN GLASSMORPHIC PALETTE ---
private val DarkBg = Color(0xFF0F172A)          // Rich Dark Navy
private val CardBg = Color(0xFF1E293B)          // Slate Surface
private val GlassBorder = Color(0xFF334155)     // Subtle Border
private val AccentBlue = Color(0xFF3B82F6)      // Electric Blue
private val AccentPurple = Color(0xFF8B5CF6)    // Soft Purple
private val AccentOrange = Color(0xFFF59E0B)    // Alert Orange
private val AccentRed = Color(0xFFEF4444)       // Critical Red
private val SuccessGreen = Color(0xFF10B981)    // Success Green
private val TextWhite = Color(0xFFF8FAFC)       // Off-White
private val TextMuted = Color(0xFF94A3B8)       // Cool Gray

@Composable
fun DashboardScreen(navController: NavController, role: String) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    val normalizedRole = remember(role) { role.lowercase() }
    val userId = remember { sessionManager.getUserId()?.replace("\"", "")?.replace("'", "") ?: "" }
    val userName = remember { sessionManager.getName() ?: "User" }
    
    var studentHistory by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var facultySessions by remember { mutableStateOf<List<FacultySessionRecord>>(emptyList()) }
    var todaySchedule by remember { mutableStateOf<List<ScheduleRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentDay = remember {
        val calendar = Calendar.getInstance()
        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
    }

    // --- CONNECTION LOGIC (UNTOUCHED) ---
    LaunchedEffect(userId, normalizedRole) {
        if (userId.isNotEmpty()) {
            try {
                coroutineScope {
                    if (normalizedRole == "student") {
                        val historyJob = async { RetrofitClient.apiService.getAttendanceHistory(userId) }
                        val scheduleJob = async { RetrofitClient.apiService.getStudentSchedule(userId, currentDay) }
                        
                        val historyRes = historyJob.await()
                        if (historyRes.isSuccessful) studentHistory = historyRes.body() ?: emptyList()
                        
                        val scheduleRes = scheduleJob.await()
                        if (scheduleRes.isSuccessful) todaySchedule = scheduleRes.body() ?: emptyList()
                    } else if (normalizedRole == "faculty") {
                        val sessionsJob = async { RetrofitClient.apiService.getFacultySessions(userId) }
                        val scheduleJob = async { RetrofitClient.apiService.getFacultySchedule(userId, currentDay) }
                        
                        val sessionsRes = sessionsJob.await()
                        if (sessionsRes.isSuccessful) facultySessions = sessionsRes.body() ?: emptyList()
                        
                        val scheduleRes = scheduleJob.await()
                        if (scheduleRes.isSuccessful) todaySchedule = scheduleRes.body() ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardScreen", "Error fetching dashboard data", e)
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController, role) },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue, strokeWidth = 3.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Top Gradient Header
                    item {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(AccentBlue.copy(alpha = 0.15f), Color.Transparent))
                            )
                            .statusBarsPadding()
                            .padding(20.dp)
                        ) {
                            HeaderSection(navController)
                        }
                    }

                    // Greeting
                    item {
                        GreetingSection(normalizedRole, userName, userId, modifier = Modifier.padding(horizontal = 24.dp))
                    }

                    // Stats Section
                    item {
                        if (normalizedRole == "student") {
                            StudentStatsRow(studentHistory)
                        } else {
                            FacultyStatsRow(facultySessions, todaySchedule)
                        }
                    }

                    // Actions Section
                    item {
                        if (normalizedRole == "student") {
                            QuickActionsSection(navController, modifier = Modifier.padding(horizontal = 24.dp))
                        } else if (normalizedRole == "faculty") {
                            FacultyManagementSection(navController, modifier = Modifier.padding(horizontal = 24.dp))
                        }
                    }

                    // Main Content Cards
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (normalizedRole == "student") {
                                    ModernRecentAttendanceCard(
                                        modifier = Modifier.weight(1f),
                                        history = studentHistory
                                    )
                                } else {
                                    ModernRecentSessionsCard(
                                        modifier = Modifier.weight(1f),
                                        sessions = facultySessions
                                    )
                                }
                                ModernScheduleCard(
                                    modifier = Modifier.weight(1f),
                                    schedule = todaySchedule,
                                    onViewAll = {
                                        if (normalizedRole == "faculty") navController.navigate("faculty_classes")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating "Mark Attendance" for Students
            if (normalizedRole == "student") {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("mark_attendance") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 24.dp, end = 16.dp)
                        .shadow(12.dp, CircleShape, spotColor = AccentBlue),
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.QrCodeScanner, null) },
                    text = { Text("Mark Attendance", fontWeight = FontWeight.Bold) }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { 
                sessionManager.logout()
                navController.navigate("role_selection") {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier.size(44.dp).background(CardBg, CircleShape).border(1.dp, GlassBorder, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = AccentRed, modifier = Modifier.size(20.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /* Notifications */ }) {
                Icon(Icons.Outlined.Notifications, null, tint = TextMuted)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple)))
                    .clickable { navController.navigate("profile") }
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = TextWhite)
            }
        }
    }
}

@Composable
fun GreetingSection(role: String, name: String, userId: String, modifier: Modifier = Modifier) {
    val subtext = when (role.lowercase()) {
        "student" -> "S.Y. B.Tech (I.T.) • $userId"
        "faculty" -> "Information Technology Dept."
        else -> "Portal Access"
    }
    Column(modifier = modifier) {
        Text(
            text = "Hello,",
            fontSize = 16.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = name,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextWhite,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = AccentBlue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.wrapContentSize()
        ) {
            Text(
                text = subtext,
                fontSize = 12.sp,
                color = AccentBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun StudentStatsRow(history: List<AttendanceRecord>) {
    val total = history.size
    val present = history.count { it.status.lowercase() == "present" }
    val percentage = if (total > 0) (present.toDouble() / total * 100) else 0.0
    val formattedPercent = "%.1f%%".format(percentage)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item { ModernStatCard("Attendance", formattedPercent, if (percentage >= 75) "Stable" else "At Risk", if (percentage >= 75) SuccessGreen else AccentRed) }
        item { ModernStatCard("Present", present.toString(), "Sessions", AccentBlue) }
        item { ModernStatCard("Missed", (total - present).toString(), "Sessions", AccentOrange) }
    }
}

@Composable
fun FacultyStatsRow(sessions: List<FacultySessionRecord>, schedule: List<ScheduleRecord>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item { ModernStatCard("Classes", schedule.size.toString(), "Today", AccentBlue) }
        item { ModernStatCard("Sessions", sessions.size.toString(), "Conducted", SuccessGreen) }
        item { ModernStatCard("Alerts", "0", "Students", AccentOrange) }
    }
}

@Composable
fun ModernStatCard(title: String, value: String, subtitle: String, color: Color) {
    Card(
        modifier = Modifier.width(130.dp).height(160.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Column {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black, color = TextWhite)
                Text(title, fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickActionsSection(navController: NavController, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernActionItem("History", Icons.Default.History, AccentBlue, Modifier.weight(1f)) {
                navController.navigate("attendance_history")
            }
            ModernActionItem("Schedule", Icons.Default.CalendarMonth, SuccessGreen, Modifier.weight(1f)) {
                 /* TODO */
            }
            ModernActionItem("More", Icons.Default.MoreHoriz, TextMuted, Modifier.weight(1f)) {
                navController.navigate("profile")
            }
        }
    }
}

@Composable
fun FacultyManagementSection(navController: NavController, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernActionItem("Start", Icons.Default.AddCircle, AccentBlue, Modifier.weight(1f)) {
                navController.navigate("start_session")
            }
            ModernActionItem("History", Icons.Default.BarChart, SuccessGreen, Modifier.weight(1f)) {
                navController.navigate("faculty_history")
            }
            ModernActionItem("Classes", Icons.Default.School, AccentPurple, Modifier.weight(1f)) {
                navController.navigate("faculty_classes")
            }
        }
    }
}

@Composable
fun ModernActionItem(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(label, fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ModernRecentAttendanceCard(modifier: Modifier = Modifier, history: List<AttendanceRecord>) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Latest Log", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                val latest = history.first()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(latest.subject_id, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(latest.timestamp.take(10), fontSize = 12.sp, color = TextMuted)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = if (latest.status.lowercase() == "present") SuccessGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            latest.status.uppercase(),
                            color = if (latest.status.lowercase() == "present") SuccessGreen else AccentRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernRecentSessionsCard(modifier: Modifier = Modifier, sessions: List<FacultySessionRecord>) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Last Class", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sessions", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                val latest = sessions.first()
                Column {
                    Text(latest.subject_id, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextWhite, maxLines = 1)
                    Text("${latest.student_count} Present", fontSize = 14.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("COMPLETED", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ModernScheduleCard(modifier: Modifier = Modifier, schedule: List<ScheduleRecord>, onViewAll: () -> Unit) {
    Card(
        modifier = modifier.height(200.dp).clickable { onViewAll() },
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Up Next", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (schedule.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Free Day", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                val next = schedule.first()
                Column {
                    Text(next.time, fontSize = 11.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                    Text(next.subject, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(next.room, fontSize = 12.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, role: String) {
    NavigationBar(
        containerColor = Color(0xFF111827),
        tonalElevation = 0.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .height(72.dp)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        val currentRoute = "dashboard/${role.lowercase()}"
        val items = when (role.lowercase()) {
            "student" -> {
                listOf(
                    Triple("Home", Icons.Default.GridView, "dashboard/student"),
                    Triple("Log", Icons.AutoMirrored.Filled.Assignment, "attendance_history"),
                    Triple("Alert", Icons.Default.NotificationsNone, "alerts")
                )
            }
            "faculty" -> {
                listOf(
                    Triple("Home", Icons.Default.GridView, "dashboard/faculty"),
                    Triple("Manage", Icons.Default.Dataset, "faculty_classes"),
                    Triple("History", Icons.Default.History, "faculty_history")
                )
            }
            "hod" -> {
                listOf(
                    Triple("Home", Icons.Default.GridView, "dashboard/hod"),
                    Triple("Analytics", Icons.Default.BarChart, "hod_analytics"),
                    Triple("Manage", Icons.Default.Domain, "hod_manage")
                )
            }
            else -> {
                listOf(
                    Triple("Home", Icons.Default.GridView, "dashboard/$role")
                )
            }
        }

        items.forEach { (label, icon, route) ->
            val selected = currentRoute == route
            NavigationBarItem(
                icon = { Icon(icon, null, modifier = Modifier.size(24.dp)) },
                label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                selected = selected,
                onClick = { if (route.isNotEmpty()) navController.navigate(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentBlue,
                    selectedTextColor = AccentBlue,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
