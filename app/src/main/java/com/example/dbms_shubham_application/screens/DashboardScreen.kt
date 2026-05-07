package com.example.dbms_shubham_application.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.AttendanceRecord
import com.example.dbms_shubham_application.data.model.FacultySessionRecord
import com.example.dbms_shubham_application.data.model.ScheduleRecord
import com.example.dbms_shubham_application.data.model.SubjectAttendance
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.components.DashboardShimmer
import com.example.dbms_shubham_application.ui.components.HODActionStripItem
import com.example.dbms_shubham_application.ui.components.LiveSessionPulse
import com.example.dbms_shubham_application.ui.components.ProfessionalStatStrip
import com.example.dbms_shubham_application.ui.components.StatStripItem
import com.example.dbms_shubham_application.ui.components.VerticalStripDivider
import com.example.dbms_shubham_application.ui.components.shimmerEffect
import com.example.dbms_shubham_application.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// --- MODERN GLASSMORPHIC PALETTE ---
// Removed hardcoded colors, using MaterialTheme.colorScheme instead

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, role: String) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    val normalizedRole = remember(role) { role.lowercase() }
    val userId = remember { sessionManager.getUserId()?.replace("\"", "")?.replace("'", "") ?: "" }
    val userName = remember { sessionManager.getName() ?: "User" }
    
    var studentHistory by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var subjectAttendance by remember { mutableStateOf<List<SubjectAttendance>>(emptyList()) }
    var facultySessions by remember { mutableStateOf<List<FacultySessionRecord>>(emptyList()) }
    var todaySchedule by remember { mutableStateOf<List<ScheduleRecord>>(emptyList()) }
    var unreadNotificationsCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var deptAnalytics by remember { mutableStateOf<com.example.dbms_shubham_application.data.model.DepartmentAnalytics?>(null) }

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
    
    // Use a key for refreshing
    var refreshCount by remember { mutableIntStateOf(0) }

    // --- CONNECTION LOGIC (UPDATED) ---
    LaunchedEffect(userId, normalizedRole, refreshCount) {
        if (userId.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        try {
            coroutineScope {
                // Start fetching data in parallel
                val profileDeferred = async { RetrofitClient.apiService.getUserProfile(userId) }
                val notifDeferred = async { RetrofitClient.apiService.getNotifications(userId) }
                
                when (normalizedRole) {
                    "student" -> {
                        val historyDef = async { RetrofitClient.apiService.getAttendanceHistory(userId) }
                        val subjectDef = async { RetrofitClient.apiService.getSubjectAttendance(userId) }
                        val scheduleDef = async { RetrofitClient.apiService.getStudentSchedule(userId, currentDay) }
                        
                        historyDef.await().let { res ->
                            if (res.isSuccessful) studentHistory = res.body()?.filter { !it.session_id.startsWith("cloud___") } ?: emptyList()
                        }
                        subjectDef.await().let { res ->
                            if (res.isSuccessful) subjectAttendance = res.body() ?: emptyList()
                        }
                        scheduleDef.await().let { res ->
                            if (res.isSuccessful) todaySchedule = res.body() ?: emptyList()
                        }
                    }
                    "faculty" -> {
                        val scheduleDef = async { RetrofitClient.apiService.getFacultySchedule(userId, currentDay) }
                        val sessionsDef = async { RetrofitClient.apiService.getFacultySessions(userId) }
                        
                        scheduleDef.await().let { res ->
                            if (res.isSuccessful) todaySchedule = res.body() ?: emptyList()
                        }
                        sessionsDef.await().let { res ->
                            if (res.isSuccessful) facultySessions = res.body()?.filter { !it.session_id.startsWith("cloud___") } ?: emptyList()
                        }
                    }
                }

                profileDeferred.await().let { res ->
                    if (res.isSuccessful) {
                        val profile = res.body()
                        userProfile = profile
                        // If HOD, fetch department analytics
                        if (normalizedRole == "hod") {
                            val branch = profile?.academic?.get("branch") ?: ""
                            if (branch.isNotEmpty()) {
                                val analyticsRes = RetrofitClient.apiService.getDepartmentAnalytics(branch)
                                if (analyticsRes.isSuccessful) {
                                    deptAnalytics = analyticsRes.body()
                                }
                            }
                        }
                    }
                }
                notifDeferred.await().let { res ->
                    if (res.isSuccessful) unreadNotificationsCount = res.body()?.count { !it.is_read } ?: 0
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Error fetching dashboard data", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController, role) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading && refreshCount > 0,
            onRefresh = { 
                isLoading = true
                refreshCount++
            },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            if (isLoading && refreshCount == 0) {
                DashboardShimmer()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Top Gradient Header
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
                        ) {
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
                    }

                    // Greeting
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
                        ) {
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
                    }

                    // Live Session Pulse for Faculty
                    if (normalizedRole == "faculty") {
                        val activeSession = facultySessions.find { it.status.lowercase() == "active" }
                        if (activeSession != null) {
                            item {
                                LiveSessionPulse(activeSession) {
                                    navController.navigate("start_session") // Or a specific session view
                                }
                            }
                        }
                    }

                    // Stats Section
                    item {
                        if (normalizedRole == "student") {
                            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                                StudentStatsRow(studentHistory, isLoading)
                                SubjectAttendanceSection(subjectAttendance, isLoading, navController)
                            }
                        } else if (normalizedRole == "hod") {
                            HODStatsSection(isLoading, deptAnalytics)
                        } else {
                            FacultyStatsRow(facultySessions, todaySchedule, isLoading)
                        }
                    }

                    // Actions Section
                    item {
                        when (normalizedRole) {
                            "student" -> QuickActionsSection(navController, modifier = Modifier.padding(horizontal = 24.dp))
                            "faculty" -> FacultyManagementSection(navController, modifier = Modifier.padding(horizontal = 24.dp))
                            "hod" -> HODActionsSection(
                                navController = navController, 
                                modifier = Modifier.padding(horizontal = 24.dp),
                                branch = userProfile?.academic?.get("branch") ?: ""
                            )
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
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Institutional Branding
        Text(
            "Veermata Jijabai Technological Institute",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
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
}

@Composable
fun GreetingSection(role: String, name: String, subtext: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (role == "student") Icons.Default.School else Icons.Default.Badge,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Premium Dynamic Role Badge
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                )
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = role.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun StudentStatsRow(history: List<AttendanceRecord>, isLoading: Boolean = false) {
    val total = history.size
    val present = history.count { it.status.lowercase() == "present" }
    val percentage = if (total > 0) (present.toDouble() / total) else 0.0
    val displayPercent = (percentage * 100).toInt()

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Attendance Health", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isLoading) Modifier.shimmerEffect() else Modifier),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), Color.Transparent)
                )
            )) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { percentage.toFloat() },
                            modifier = Modifier.size(85.dp),
                            color = if (percentage >= 0.75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = "$displayPercent%",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    Column {
                        Text(
                            text = if (percentage >= 0.75) "Looking Great!" else "Attention Needed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "You have attended $present out of $total sessions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (present.toFloat() / (total.coerceAtLeast(1))) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = if (percentage >= 0.75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }
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
fun FacultyStatsRow(sessions: List<FacultySessionRecord>, schedule: List<ScheduleRecord>, isLoading: Boolean = false) {
    val totalSessions = sessions.size
    val todayClasses = schedule.size
    
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Teaching Overview", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's Load Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .then(if (isLoading) Modifier.shimmerEffect() else Modifier),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                if (!isLoading) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.AutoGraph, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(todayClasses.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
                        Text("Classes Today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
            
            // Monthly Impact Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .then(if (isLoading) Modifier.shimmerEffect() else Modifier),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
            ) {
                if (!isLoading) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.People, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(totalSessions.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
                        Text("Total Sessions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
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
            ModernActionItem("Leaves", Icons.AutoMirrored.Filled.EventNote, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) {
                navController.navigate("leave_management")
            }
        }
    }
}

@Composable
fun FacultyManagementSection(navController: NavController, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Management Hub", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Start Session Action - Premium Gradient
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .clickable { navController.navigate("start_session") },
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 12.dp
            ) {
                Box(modifier = Modifier.background(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent))
                )) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Column {
                            Text("New Session", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
                            Text("Start live QR class", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
            }

            // Reports Action - Sophisticated Look
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .clickable { navController.navigate("reports") },
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Assessment, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(26.dp))
                    }
                    Column {
                        Text("Reports", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp)
                        Text("Analytics & PDF", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HODStatsSection(isLoading: Boolean, analytics: com.example.dbms_shubham_application.data.model.DepartmentAnalytics?) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)) + expandVertically(animationSpec = tween(600))
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Department Overview", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfessionalStatStrip {
                StatStripItem(
                    label = "Avg. Att.",
                    value = analytics?.avg_attendance ?: "--%",
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                VerticalStripDivider()
                StatStripItem(
                    label = "Faculty",
                    value = (analytics?.total_faculty ?: 0).toString(),
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                VerticalStripDivider()
                StatStripItem(
                    label = "Students",
                    value = String.format("%02d", analytics?.total_students ?: 0),
                    icon = Icons.Default.Group,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun HODActionsSection(navController: NavController, modifier: Modifier = Modifier, branch: String = "") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    fun downloadDepartmentRecords() {
        if (branch.isEmpty()) {
            Toast.makeText(context, "Department information missing", Toast.LENGTH_SHORT).show()
            return
        }
        isExporting = true
        scope.launch(Dispatchers.IO) {
            try {
                // Professional master data export
                val response = RetrofitClient.apiService.downloadHodMasterExcel(departmentId = branch)
                if (response.isSuccessful) {
                    response.body()?.let { 
                        FileUtils.saveFile(
                            it.byteStream(), 
                            "Dept_${branch}_Master_Records.xlsx",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                            context
                        )
                    }
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "Records Exported Successfully", Toast.LENGTH_LONG).show()
                    }
                } else {
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "Export failed: No data found", Toast.LENGTH_SHORT).show() 
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show() 
                }
            } finally {
                withContext(Dispatchers.Main) { isExporting = false }
            }
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = modifier) {
        Text("Departmental Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(800)) + slideInVertically(initialOffsetY = { 40 })
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column {
                    HODActionStripItem(
                        label = "Department Analytics",
                        icon = Icons.Default.BarChart,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        navController.navigate("hod_analytics")
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(0.1f))
                    
                    HODActionStripItem(
                        label = if (isExporting) "Generating Report..." else "Export Master Records",
                        icon = if (isExporting) Icons.Default.HourglassEmpty else Icons.Default.FileDownload,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        if (!isExporting) downloadDepartmentRecords()
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(0.1f))
                    HODActionStripItem(
                        label = "Manage Faculty",
                        icon = Icons.Default.Domain,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    ) {
                        navController.navigate("hod_manage")
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(0.1f))
                    HODActionStripItem(
                        label = "Broadcasting Notify",
                        icon = Icons.Default.Campaign,
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        navController.navigate("send_notification")
                    }
                }
            }
        }
    }
}

@Composable
fun ModernActionItem(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
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
                    Text(
                        text = latest.subject_name.ifEmpty { latest.subject_id },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                val displayName = latest.subject_name.ifEmpty { latest.subject_id }
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
fun SubjectAttendanceSection(attendance: List<SubjectAttendance>, isLoading: Boolean, navController: NavController) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Subject-wise Analysis",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "${attendance.size} Subjects",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading && attendance.isEmpty()) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(bottom = 12.dp)
                        .shimmerEffect()
                )
            }
        } else if (attendance.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No subject data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 24.dp)
            ) {
                items(attendance.size) { index ->
                    val item = attendance[index]
                    SubjectAttendanceCard(item, navController)
                }
            }
        }
    }
}

@Composable
fun SubjectAttendanceCard(item: SubjectAttendance, navController: NavController?) {
    val color = when {
        item.percentage >= 0.75 -> MaterialTheme.colorScheme.primary
        item.percentage >= 0.65 -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(180.dp)
            .clickable { 
                val route = "subject_details/${item.subject_id}/${item.subject_name}/${item.percentage.toFloat()}/${item.attended_classes}/${item.total_classes}"
                navController?.navigate(route)
            },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.subject_id.take(2).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
            
            Column {
                Text(
                    item.subject_name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.attended_classes}/${item.total_classes} Classes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "${(item.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = color
                    )
                }
                LinearProgressIndicator(
                    progress = { item.percentage.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, role: String) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
