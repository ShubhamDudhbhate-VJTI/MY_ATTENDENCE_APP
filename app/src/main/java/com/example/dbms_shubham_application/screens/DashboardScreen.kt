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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
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
// Removed hardcoded colors, using MaterialTheme.colorScheme instead

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
    var unreadNotificationsCount by remember { mutableIntStateOf(0) }
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

    var userProfile by remember { mutableStateOf<com.example.dbms_shubham_application.data.model.UserProfile?>(null) }
    
    // --- CONNECTION LOGIC (UPDATED) ---
    LaunchedEffect(userId, normalizedRole) {
        if (userId.isNotEmpty()) {
            try {
                coroutineScope {
                    // Fetch user profile for branch/year details
                    val profileRes = RetrofitClient.apiService.getUserProfile(userId)
                    if (profileRes.isSuccessful) userProfile = profileRes.body()

                    if (normalizedRole == "student") {
                        val historyJob = async { RetrofitClient.apiService.getAttendanceHistory(userId) }
                        val scheduleJob = async { RetrofitClient.apiService.getStudentSchedule(userId, currentDay) }
                        val notifJob = async { RetrofitClient.apiService.getNotifications(userId) }
                        
                        val historyRes = historyJob.await()
                        if (historyRes.isSuccessful) studentHistory = historyRes.body() ?: emptyList()
                        
                        val scheduleRes = scheduleJob.await()
                        if (scheduleRes.isSuccessful) todaySchedule = scheduleRes.body() ?: emptyList()

                        val notifRes = notifJob.await()
                        if (notifRes.isSuccessful) {
                            unreadNotificationsCount = notifRes.body()?.count { !it.is_read } ?: 0
                        }
                    } else if (normalizedRole == "faculty") {
                        val sessionsJob = async { RetrofitClient.apiService.getFacultySessions(userId) }
                        val scheduleJob = async { RetrofitClient.apiService.getFacultySchedule(userId, currentDay) }
                        val notifJob = async { RetrofitClient.apiService.getNotifications(userId) }
                        
                        val sessionsRes = sessionsJob.await()
                        if (sessionsRes.isSuccessful) facultySessions = sessionsRes.body() ?: emptyList()
                        
                        val scheduleRes = scheduleJob.await()
                        if (scheduleRes.isSuccessful) todaySchedule = scheduleRes.body() ?: emptyList()

                        val notifRes = notifJob.await()
                        if (notifRes.isSuccessful) {
                            unreadNotificationsCount = notifRes.body()?.count { !it.is_read } ?: 0
                        }
                    } else if (normalizedRole == "hod") {
                         val notifJob = async { RetrofitClient.apiService.getNotifications(userId) }
                         val notifRes = notifJob.await()
                         if (notifRes.isSuccessful) {
                            unreadNotificationsCount = notifRes.body()?.count { !it.is_read } ?: 0
                         }
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
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
                                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent))
                            )
                            .statusBarsPadding()
                            .padding(20.dp)
                        ) {
                            HeaderSection(navController, unreadNotificationsCount)
                        }
                    }

                    // Greeting
                    item {
                        val branch = userProfile?.academic?.get("branch") ?: "N/A"
                        val year = userProfile?.academic?.get("year") ?: "N/A"
                        val regNo = userProfile?.academic?.get("reg_no") ?: userId
                        
                        val dynamicSubtext = if (normalizedRole == "student") {
                            "$year $branch • $regNo"
                        } else {
                            "$branch Dept. • $userId"
                        }
                        
                        GreetingSection(normalizedRole, userName, dynamicSubtext, modifier = Modifier.padding(horizontal = 24.dp))
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
                        } else if (normalizedRole == "hod") {
                            HODActionsSection(navController, modifier = Modifier.padding(horizontal = 24.dp))
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
                        .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.QrCodeScanner, null) },
                    text = { Text("Mark Attendance", fontWeight = FontWeight.Bold) }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(navController: NavController, unreadCount: Int) {
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
            modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.navigate("alerts") }) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(unreadCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
                    .clickable { navController.navigate("profile") }
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun GreetingSection(role: String, name: String, subtext: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Hello,",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = name,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.wrapContentSize()
        ) {
            Text(
                text = subtext,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
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

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Your Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernStatCard("Overall", formattedPercent, if (percentage >= 75) "Stable" else "At Risk", if (percentage >= 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, Modifier.weight(1.2f))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallStatCard("Present", present.toString(), MaterialTheme.colorScheme.primary)
                SmallStatCard("Missed", (total - present).toString(), MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun SmallStatCard(title: String, value: String, color: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
            }
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        }
    }
}

@Composable
fun FacultyStatsRow(sessions: List<FacultySessionRecord>, schedule: List<ScheduleRecord>) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Daily Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernStatCard("Today", schedule.size.toString(), "Classes", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            ModernStatCard("Total", sessions.size.toString(), "Sessions", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            ModernStatCard("Alerts", "0", "Students", MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }
    }
}

@Composable
fun ModernStatCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(115.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuickActionsSection(navController: NavController, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            TextButton(onClick = { navController.navigate("profile") }) {
                Text("View Profile", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernActionItem("Schedule", Icons.Default.CalendarMonth, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) {
                 /* TODO */
            }
            ModernActionItem("History", Icons.Default.History, MaterialTheme.colorScheme.secondary, Modifier.weight(1f)) {
                navController.navigate("attendance_history")
            }
            ModernActionItem("Resources", Icons.AutoMirrored.Filled.LibraryBooks, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) {
                /* TODO */
            }
        }
    }
}

@Composable
fun FacultyManagementSection(navController: NavController, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Single prominent Start Session action as requested
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate("start_session") },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Start New Session", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Begin tracking attendance now", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun HODActionsSection(navController: NavController, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Analytics and Manage are already in bottom bar
            ModernActionItem("Notify", Icons.Default.Notifications, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) {
                navController.navigate("send_notification")
            }
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}

@Composable
fun ModernActionItem(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ModernRecentAttendanceCard(modifier: Modifier = Modifier, history: List<AttendanceRecord>) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Latest Log", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            } else {
                val latest = history.first()
                val isPresent = latest.status.lowercase() == "present"
                val statusColor = if (isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Use readable subject name if available (even though AttendanceRecord might not have it yet, we handle it)
                    Text(latest.subject_id, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(latest.timestamp.take(10), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            latest.status.uppercase(),
                            color = statusColor,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Last Class", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sessions", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            } else {
                val latest = sessions.first()
                val displayName = if (latest.subject_name.isNotEmpty()) latest.subject_name else latest.subject_id
                Column {
                    Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${latest.student_count} Present", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("COMPLETED", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ModernScheduleCard(modifier: Modifier = Modifier, schedule: List<ScheduleRecord>, onViewAll: () -> Unit) {
    Card(
        modifier = modifier.height(200.dp).clickable { onViewAll() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Up Next", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (schedule.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Free Day", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                val next = schedule.first()
                Column {
                    Text(next.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text(next.subject, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(next.room, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, role: String) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .height(72.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
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
                    Triple("Notify", Icons.Default.Notifications, "send_notification"),
                    Triple("Classes", Icons.Default.School, "faculty_classes"),
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
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
