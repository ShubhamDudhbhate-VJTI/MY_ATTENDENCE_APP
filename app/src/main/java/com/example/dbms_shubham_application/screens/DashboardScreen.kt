package com.example.dbms_shubham_application.screens

import androidx.compose.ui.platform.LocalContext
import com.example.dbms_shubham_application.data.local.SessionManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.example.dbms_shubham_application.data.model.AttendanceRecord
import com.example.dbms_shubham_application.data.model.FacultySessionRecord
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentOrange = Color(0xFFF59E0B)
private val AccentRed = Color(0xFFEF4444)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)

@Composable
fun DashboardScreen(navController: NavController, role: String) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId = sessionManager.getUserId() ?: ""
    val scope = rememberCoroutineScope()
    
    var studentHistory by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var facultySessions by remember { mutableStateOf<List<FacultySessionRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            scope.launch {
                try {
                    if (role.lowercase() == "student") {
                        val response = RetrofitClient.apiService.getAttendanceHistory(userId)
                        if (response.isSuccessful) {
                            studentHistory = response.body() ?: emptyList()
                        }
                    } else if (role.lowercase() == "faculty") {
                        val response = RetrofitClient.apiService.getFacultySessions(userId)
                        if (response.isSuccessful) {
                            facultySessions = response.body() ?: emptyList()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val roleTitle = when (role.lowercase()) {
        "student" -> "Student Portal"
        "faculty" -> "Faculty Dashboard"
        "hod" -> "HOD Administration"
        else -> "Dashboard"
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController, role) },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                item {
                    HeaderSection(navController, roleTitle)
                }

                // Greeting
                item {
                    GreetingSection(role)
                }

                // Stats Row
                item {
                    if (role.lowercase() == "student") {
                        StudentStatsRow(studentHistory)
                    } else {
                        StatsRow(role) // Keep original for now or update later
                    }
                }

                // Quick Actions (New Section)
                if (role.lowercase() == "student") {
                    item {
                        QuickActionsSection(navController)
                    }
                } else if (role.lowercase() == "faculty") {
                    item {
                        FacultyQuickActionsSection(navController)
                    }
                }

                // Two main cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (role.lowercase() == "student") {
                            RecentAttendanceCard(
                                modifier = Modifier.weight(1f),
                                role = role,
                                history = studentHistory
                            )
                        } else {
                            RecentSessionsCard(
                                modifier = Modifier.weight(1f),
                                sessions = facultySessions
                            )
                        }
                        TodayScheduleCard(modifier = Modifier.weight(1f), role = role)
                    }
                }
            }

            // Side Mark Attendance Button (Only for students)
            if (role.lowercase() == "student") {
                MarkAttendanceSideButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 20.dp),
                    onClick = { navController.navigate("mark_attendance") }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(navController: NavController, title: String) {
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
            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = AccentRed)
        }
        
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AccentBlue, Color(0xFF8B5CF6))))
                .clickable { navController.navigate("profile") },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = TextWhite)
        }
    }
}

@Composable
fun GreetingSection(role: String) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val name = sessionManager.getName() ?: "User"
    
    val subtext = when (role.lowercase()) {
        "student" -> "S.Y. B.Tech (I.T.) • ${sessionManager.getUserId()}"
        "faculty" -> "Information Technology Department"
        "hod" -> "Head of IT Department"
        else -> ""
    }
    Column {
        Text(
            text = "Welcome back, $name",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = subtext,
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun StudentStatsRow(history: List<AttendanceRecord>) {
    val total = history.size
    val present = history.count { it.status.lowercase() == "present" }
    val percentage = if (total > 0) (present.toDouble() / total * 100) else 0.0
    val formattedPercent = "%.1f%%".format(percentage)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item { StatCard("Overall Attendance", formattedPercent, if (percentage >= 75) "Eligible" else "Low", if (percentage >= 75) AccentBlue else AccentRed) }
        item { StatCard("Sessions Attended", present.toString(), "of $total total", Color(0xFF10B981)) }
        item { StatCard("Sessions Missed", (total - present).toString(), "Total", AccentOrange) }
    }
}

@Composable
fun StatsRow(role: String) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (role.lowercase() == "student") {
            item { StatCard("Overall Attendance", "83.2%", "Eligible", AccentBlue) }
            item { StatCard("Sessions Attended", "124", "of 149 total", Color(0xFF10B981)) }
            item { StatCard("Sessions Missed", "25", "3 this week", AccentOrange) }
            item { StatCard("At-Risk Subjects", "2", "Below 75%", AccentRed) }
        } else {
            item { StatCard("Active Classes", "4", "Today", AccentBlue) }
            item { StatCard("Avg. Attendance", "88%", "Across subjects", Color(0xFF10B981)) }
            item { StatCard("Pending Alerts", "12", "Flagged students", AccentOrange) }
            item { StatCard("Upcoming Exams", "1", "Internal Assessment", AccentRed) }
        }
    }
}

@Composable
fun QuickActionsSection(navController: NavController) {
    Column {
        Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard("History", Icons.Default.History, AccentBlue, modifier = Modifier.weight(1f)) {
                navController.navigate("attendance_history")
            }
            QuickActionCard("Leave", Icons.Default.EventBusy, AccentOrange, modifier = Modifier.weight(1f)) {
                /* TODO */
            }
            QuickActionCard("Support", Icons.Default.SupportAgent, Color(0xFF8B5CF6), modifier = Modifier.weight(1f)) {
                /* TODO */
            }
        }
    }
}

@Composable
fun FacultyQuickActionsSection(navController: NavController) {
    Column {
        Text("Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard("Start Session", Icons.Default.QrCode, AccentBlue, modifier = Modifier.weight(1f)) {
                navController.navigate("start_session")
            }
            QuickActionCard("Reports", Icons.Default.BarChart, AccentOrange, modifier = Modifier.weight(1f)) {
                navController.navigate("reports")
            }
            QuickActionCard("Classes", Icons.AutoMirrored.Filled.Assignment, Color(0xFF8B5CF6), modifier = Modifier.weight(1f)) {
                navController.navigate("faculty_classes")
            }
        }
    }
}

@Composable
fun QuickActionCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun StatCard(title: String, value: String, subtitle: String, color: Color) {
    Card(
        modifier = Modifier.width(110.dp).height(150.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 12.sp, color = TextMuted, lineHeight = 14.sp)
            Column {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun RecentAttendanceCard(modifier: Modifier = Modifier, role: String, history: List<AttendanceRecord>) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recent Attendance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text("Latest records", fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (history.isEmpty()) {
                Text("No records found", color = TextMuted, fontSize = 12.sp)
            } else {
                val latest = history.first()
                val date = try {
                    if (latest.timestamp.length >= 10) {
                        val parts = latest.timestamp.substring(0, 10).split("-")
                        if (parts.size == 3) {
                            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            "${parts[2]} ${months[parts[1].toInt()]}"
                        } else latest.timestamp.take(10)
                    } else latest.timestamp
                } catch (e: Exception) {
                    latest.timestamp.take(10)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(date, fontSize = 12.sp, color = TextMuted)
                        Text(latest.subject_id, fontSize = 14.sp, color = TextWhite)
                    }
                    val isPresent = latest.status.lowercase() == "present"
                    val statusColor = if (isPresent) Color(0xFF10B981) else AccentRed
                    
                    Box(
                        modifier = Modifier.background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(latest.status, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSessionsCard(modifier: Modifier = Modifier, sessions: List<FacultySessionRecord>) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recent Sessions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text("Engagement data", fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sessions.isEmpty()) {
                Text("No sessions started", color = TextMuted, fontSize = 12.sp)
            } else {
                val latest = sessions.first()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(latest.subject_id, fontSize = 14.sp, color = TextWhite)
                        Text("${latest.student_count} Students", fontSize = 12.sp, color = AccentBlue)
                    }
                    
                    Box(
                        modifier = Modifier.background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Active", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TodayScheduleCard(modifier: Modifier = Modifier, role: String) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = if (role.lowercase() == "student") "Today's Schedule" else "Teaching Load"
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
            Text("09:00 AM", fontSize = 12.sp, color = TextMuted)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (role.lowercase() == "student") "Database Mgmt Sys" else "SE-IT Batch B", fontSize = 14.sp, color = TextWhite, modifier = Modifier.weight(1f))
                Text("R-301", fontSize = 12.sp, color = AccentBlue)
            }
        }
    }
}

@Composable
fun MarkAttendanceSideButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .width(60.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp))
            .background(AccentBlue)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            "Mark Attendance".forEach { char ->
                Text(
                    text = char.toString().uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, role: String) {
    NavigationBar(
        containerColor = Color(0xFF151F32),
        tonalElevation = 8.dp
    ) {
        val items = if (role.lowercase() == "student") {
            listOf(
                Triple("Dashboard", Icons.Default.Dashboard, "dashboard/student"),
                Triple("Attendance", Icons.AutoMirrored.Filled.Assignment, "attendance_history"),
                Triple("Mark", Icons.Default.QrCodeScanner, "mark_attendance"),
                Triple("Alerts", Icons.Default.Notifications, "alerts")
            )
        } else {
            listOf(
                Triple("Dashboard", Icons.Default.Dashboard, "dashboard/$role"),
                Triple("My Classes", Icons.AutoMirrored.Filled.Assignment, "faculty_classes"),
                Triple("Reports", Icons.Default.BarChart, "reports"),
                Triple("Alerts", Icons.Default.Notifications, "alerts")
            )
        }

        items.forEach { (label, icon, route) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                selected = route == "dashboard/$role",
                onClick = { 
                    if (route.isNotEmpty()) {
                        navController.navigate(route)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    selectedIconColor = AccentBlue,
                    indicatorColor = AccentBlue.copy(alpha = 0.1f)
                )
            )
        }
    }
}


data class AttendanceStats(
    val totalClasses: Int,
    val presentClasses: Int,
    val absentClasses: Int,
    val percentage: Double
)


data class ScheduleItem(
    val subject: String,
    val time: String,
    val status: String
)
