@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.dbms_shubham_application.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.*
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.utils.DateTimeUtils
import com.example.dbms_shubham_application.utils.FileUtils
import com.example.dbms_shubham_application.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportsScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId = sessionManager.getUserId() ?: ""
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var reports by remember { mutableStateOf<List<FacultySessionRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Details State
    var selectedSessionDetails by remember { mutableStateOf<SessionDetailsResponse?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    // Analytics State
    var analyticsData by remember { mutableStateOf<FacultyAnalytics?>(null) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var isLoadingAnalytics by remember { mutableStateOf(false) }

    // Dropdown States
    val branches = listOf("All", "IT", "Computer", "Mechanical", "Civil", "ENTC")
    val years = listOf("All", "First Year", "Second Year", "Third Year", "Final Year")
    var subjects by remember { mutableStateOf(listOf<Subject>()) }
    
    var selectedBranch by remember { mutableStateOf("All") }
    var selectedYear by remember { mutableStateOf("All") }
    var selectedSubjectId by remember { mutableStateOf("All") }
    var studentRollNo by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    // Summary State
    var summaryCount by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isLoadingSummary by remember { mutableStateOf(false) }

    fun fetchSummary() {
        scope.launch {
            isLoadingSummary = true
            try {
                val response = RetrofitClient.apiService.getReportsSummary(
                    facultyId = userId,
                    branch = selectedBranch,
                    year = selectedYear,
                    subjectId = selectedSubjectId,
                    studentId = studentRollNo.takeIf { it.isNotEmpty() },
                    startDate = startDate.takeIf { it.isNotEmpty() },
                    endDate = endDate.takeIf { it.isNotEmpty() }
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    summaryCount = (body?.get("total_sessions") ?: 0) to (body?.get("total_students") ?: 0)
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { isLoadingSummary = false }
        }
    }

    // Update summary when filters change
    LaunchedEffect(selectedBranch, selectedYear, selectedSubjectId, studentRollNo, startDate, endDate) {
        if (selectedTab == 1) fetchSummary()
    }

    val colorScheme = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun fetchSessions() {
        if (userId.isEmpty()) return
        scope.launch {
            isRefreshing = true
            if (reports.isEmpty()) isLoading = true
            try {
                val response = RetrofitClient.apiService.getFacultySessions(userId)
                if (response.isSuccessful) {
                    val allSessions = response.body() ?: emptyList()
                    val sorted = allSessions.sortedByDescending { it.start_time }

                    if (sorted.isNotEmpty()) {
                        // Keep the absolute latest session (so 'NEW' always works)
                        val latest = sorted.first()
                        // Filter others to only show sessions where students were actually present
                        val othersWithAttendance = sorted.drop(1).filter { it.student_count > 0 }

                        // Take the latest session + top 4 sessions with attendance = Best 5
                        reports = (listOf(latest) + othersWithAttendance).take(5)
                    } else {
                        reports = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("ReportsScreen", "Fetch failed", e)
            } finally {
                isRefreshing = false
                isLoading = false
            }
        }
    }

    fun fetchSessionDetails(sessionId: String) {
        scope.launch {
            isLoadingDetails = true
            showDetailsDialog = true
            try {
                val response = RetrofitClient.apiService.getSessionDetails(sessionId)
                if (response.isSuccessful) {
                    selectedSessionDetails = response.body()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingDetails = false
            }
        }
    }

    fun downloadPdf(sessionId: String, fileName: String, isBulk: Boolean = false) {
        isDownloading = if (isBulk) "BULK" else sessionId
        scope.launch(Dispatchers.IO) {
            try {
                val response = if (isBulk) {
                    RetrofitClient.apiService.downloadBulkReportPdf(
                        facultyId = userId,
                        branch = selectedBranch,
                        year = selectedYear,
                        subjectId = selectedSubjectId,
                        startDate = startDate.takeIf { it.isNotEmpty() },
                        endDate = endDate.takeIf { it.isNotEmpty() },
                        studentId = studentRollNo.takeIf { it.isNotEmpty() }
                    )
                } else {
                    RetrofitClient.apiService.downloadReportPdf(sessionId)
                }

                if (response.isSuccessful) {
                    response.body()?.let { FileUtils.saveFile(it.byteStream(), fileName, "application/pdf", context) }
                } else {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "No records found", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show() }
            } finally {
                withContext(Dispatchers.Main) { isDownloading = null }
            }
        }
    }

    fun fetchFacultyAnalytics() {
        scope.launch {
            isLoadingAnalytics = true
            showAnalyticsDialog = true
            try {
                val response = RetrofitClient.apiService.getFacultyAnalytics(userId)
                if (response.isSuccessful) {
                    analyticsData = response.body()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Analytics Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingAnalytics = false
            }
        }
    }

    fun deleteSession(sessionId: String) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.deleteSession(sessionId)
                if (response.isSuccessful) {
                    fetchSessions()
                    Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearAllSessions() {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.clearAllSessions(userId)
                if (response.isSuccessful) {
                    fetchSessions()
                    Toast.makeText(context, "All logs cleared", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Clear failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) fetchSessions() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            fetchSessions()
            try {
                val resp = RetrofitClient.apiService.getFacultySubjects(userId)
                if (resp.isSuccessful) subjects = resp.body() ?: emptyList()
            } catch (e: Exception) { }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        // Advanced Decorative Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Main Top Right Glow
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to colorScheme.primary.copy(0.12f),
                    1.0f to Color.Transparent
                ),
                radius = 500.dp.toPx(),
                center = Offset(size.width * 0.9f, 0f)
            )
            // Secondary Bottom Left Glow
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to colorScheme.secondary.copy(0.08f),
                    1.0f to Color.Transparent
                ),
                radius = 400.dp.toPx(),
                center = Offset(size.width * 0.1f, size.height * 0.9f)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // PREMIUM GLASSMORPHIC HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(colorScheme.surface.copy(0.95f), colorScheme.background.copy(0.9f))
                        )
                    )
                    .padding(top = 54.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.surface,
                        border = BorderStroke(1.dp, colorScheme.outline.copy(0.1f)),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(600)) + slideInHorizontally(initialOffsetX = { -20 }, animationSpec = tween(600))
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Audit Intelligence", fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = (-1).sp)
                            Text("System Performance & Logs", color = colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeaderAction(Icons.Default.Analytics, colorScheme.primary) { fetchFacultyAnalytics() }
                        Spacer(Modifier.width(12.dp))
                        HeaderAction(Icons.Default.DeleteSweep, colorScheme.error) { clearAllSessions() }
                        Spacer(Modifier.width(12.dp))
                        HeaderAction(Icons.Default.Refresh, colorScheme.secondary) { fetchSessions() }
                    }
                }
            }

            // PREMIUM TAB SWITCHER
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 200)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, 200))
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colorScheme.surfaceVariant.copy(0.4f))
                        .padding(4.dp)
                ) {
                    val transition = updateTransition(selectedTab, label = "TabTransition")
                    val indicatorOffset by transition.animateDp(label = "IndicatorOffset") { if (it == 0) 0.dp else 165.dp } // Approximation

                    // Sliding Indicator
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .padding(horizontal = 2.dp)
                            .offset(x = indicatorOffset)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .background(colorScheme.primary, RoundedCornerShape(16.dp))
                    )

                    Row(modifier = Modifier.fillMaxSize()) {
                        TabItem(Modifier.weight(1f), "Session Logs", selectedTab == 0) { selectedTab = 0 }
                        TabItem(Modifier.weight(1f), "Audit Center", selectedTab == 1) { selectedTab = 1 }
                    }
                }
            }

            AnimatedContent(targetState = selectedTab, transitionSpec = { fadeIn() togetherWith fadeOut() }) { targetTab ->
                if (targetTab == 0) {
                    HistoryView(
                        reports = reports,
                        isRefreshing = isRefreshing,
                        isLoading = isLoading,
                        isDownloading = isDownloading,
                        onRefresh = ::fetchSessions,
                        onDownload = ::downloadPdf,
                        onDetails = ::fetchSessionDetails,
                        onDelete = ::deleteSession
                    )
                } else {
                    AuditCenterView(
                        branches = branches,
                        years = years,
                        subjects = subjects,
                        selectedBranch = selectedBranch,
                        selectedYear = selectedYear,
                        selectedSubjectId = selectedSubjectId,
                        studentRollNo = studentRollNo,
                        startDate = startDate,
                        endDate = endDate,
                        isDownloading = isDownloading == "BULK",
                        onBranchChange = { selectedBranch = it },
                        onYearChange = { selectedYear = it },
                        onSubjectChange = { selectedSubjectId = it },
                        onRollNoChange = { studentRollNo = it },
                        onStartDateChange = { startDate = it },
                        onEndDateChange = { endDate = it },
                        onGenerate = { downloadPdf("", "Consolidated_Audit_${System.currentTimeMillis()}.pdf", true) },
                        summaryCount = summaryCount,
                        isLoadingSummary = isLoadingSummary
                    )
                }
            }
        }
    }

    if (showDetailsDialog) {
        ModernDetailsDialog(details = selectedSessionDetails, isLoading = isLoadingDetails, onDismiss = { showDetailsDialog = false })
    }

    if (showAnalyticsDialog) {
        FacultyAnalyticsDialog(analytics = analyticsData, isLoading = isLoadingAnalytics, onDismiss = { showAnalyticsDialog = false })
    }
}

@Composable
fun FacultyAnalyticsDialog(
    analytics: FacultyAnalytics?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        content = {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Performance Analytics",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                "Your aggregate teaching impact",
                                fontSize = 12.sp,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd).background(colorScheme.surfaceVariant.copy(0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colorScheme.primary, strokeWidth = 5.dp)
                        }
                    } else if (analytics == null) {
                        Text("Unable to load analytics data.", color = colorScheme.error)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Key Stats
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatsBox(Modifier.weight(1f), "Avg Attendance", analytics.avg_attendance, Icons.Default.TrendingUp)
                                StatsBox(Modifier.weight(1f), "Sessions", "${analytics.total_classes}", Icons.Default.BarChart)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatsBox(Modifier.weight(1f), "Defaulters", "${analytics.defaulter_count}", Icons.Default.TrendingDown)
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = colorScheme.primary.copy(0.05f),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                        Text("Impact Score: 8.5/10", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colorScheme.primary)
                                    }
                                }
                            }

                            // Trend Chart (Simplified version of HOD chart)
                            Card(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(0.3f)),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("MONTHLY TRENDS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = colorScheme.primary, letterSpacing = 1.5.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        analytics.trends.forEach { trend ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("${trend.value}%", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(30.dp)
                                                        .fillMaxHeight(trend.value.toFloat() / 100f)
                                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                        .background(colorScheme.primary)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(trend.month, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Close Analysis", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    )
}

@Composable
fun TabItem(modifier: Modifier, title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier.fillMaxHeight().clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun HeaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).background(color.copy(0.1f), CircleShape)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun HistoryView(
    reports: List<FacultySessionRecord>,
    isRefreshing: Boolean,
    isLoading: Boolean,
    isDownloading: String?,
    onRefresh: () -> Unit,
    onDownload: (String, String) -> Unit,
    onDetails: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        if (isLoading && reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = colorScheme.primary, strokeWidth = 5.dp) }
        } else if (reports.isEmpty()) {
            EmptyStateView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp)
            ) {
                // STATS HEADER FOR HISTORY
                item {
                    HistoryStatsHeader(reports)
                }
                
                item { Spacer(Modifier.height(20.dp)) }
                
                items(
                    items = reports,
                    key = { it.session_id }
                ) { session ->
                    val index = reports.indexOf(session)
                    ModernReportCard(
                        session = session,
                        isDownloading = isDownloading == session.session_id,
                        modifier = Modifier.animateItem(),
                        isNew = index == 0,
                        onDownload = { onDownload(session.session_id, "Attendance_${session.subject_name}_${session.session_id.take(5)}.pdf") },
                        onClick = { onDetails(session.session_id) },
                        onDelete = { onDelete(session.session_id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryStatsHeader(reports: List<FacultySessionRecord>) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "OVERVIEW PERFORMANCE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = colorScheme.primary,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Total Logs",
                value = "${reports.size}",
                icon = Icons.Default.Description,
                color = colorScheme.primary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Attendance",
                value = "${reports.sumOf { it.student_count }}",
                icon = Icons.Default.Groups,
                color = Color(0xFF00C853)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Live",
                value = "${reports.count { it.status.lowercase().contains("active") }}",
                icon = Icons.Default.Stream,
                color = Color(0xFFFFAB00)
            )
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(0.1f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.5).sp)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
        }
    }
}

@Composable
fun AuditCenterView(
    branches: List<String>,
    years: List<String>,
    subjects: List<Subject>,
    selectedBranch: String,
    selectedYear: String,
    selectedSubjectId: String,
    studentRollNo: String,
    startDate: String,
    endDate: String,
    isDownloading: Boolean,
    onBranchChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onRollNoChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onGenerate: () -> Unit,
    summaryCount: Pair<Int, Int>?,
    isLoadingSummary: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, colorScheme.outline.copy(0.05f))
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp, 24.dp).background(colorScheme.primary, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(12.dp))
                    Text("Filtration Intelligence", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                
                FilterDropdown(label = "Target Branch", options = branches, selected = selectedBranch, onSelect = onBranchChange)
                FilterDropdown(label = "Academic Year", options = years, selected = selectedYear, onSelect = onYearChange)

                val subjectNames = listOf("All") + subjects.map { it.name }
                val currentSubName = subjects.find { it.id == selectedSubjectId }?.name ?: "All"
                FilterDropdown(label = "Target Subject", options = subjectNames, selected = currentSubName) { name ->
                    onSubjectChange(if (name == "All") "All" else subjects.find { it.name == name }?.id ?: "All")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DatePickerField(label = "From Date", value = startDate, modifier = Modifier.weight(1f), onDateSelected = onStartDateChange)
                    DatePickerField(label = "To Date", value = endDate, modifier = Modifier.weight(1f), onDateSelected = onEndDateChange)
                }

                OutlinedTextField(
                    value = studentRollNo,
                    onValueChange = onRollNoChange,
                    label = { Text("Student Roll No (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.PersonSearch, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.05f))

                // Summary Preview in Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuditSummaryItem("Sessions", summaryCount?.first?.toString() ?: "0", Icons.Default.Event, isLoadingSummary)
                    VerticalDivider(modifier = Modifier.height(24.dp), color = colorScheme.outline.copy(alpha = 0.1f))
                    AuditSummaryItem("Students", summaryCount?.second?.toString() ?: "0", Icons.Default.Groups, isLoadingSummary)
                }
            }
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().height(68.dp).shadow(16.dp, RoundedCornerShape(20.dp), spotColor = colorScheme.primary),
            shape = RoundedCornerShape(20.dp),
            enabled = !isDownloading,
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
        ) {
            if (isDownloading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            } else {
                Icon(Icons.Default.AutoGraph, null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Compile Audit Intelligence", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun AuditSummaryItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isLoading: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
        }
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp).padding(top = 2.dp), strokeWidth = 2.dp)
        } else {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun EmptyStateView() {
    val colorScheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(160.dp),
                shape = CircleShape,
                color = colorScheme.primary.copy(0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoStories, null, tint = colorScheme.primary.copy(0.2f), modifier = Modifier.size(80.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Digital Silence", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("No session records found for your account.", color = colorScheme.onSurfaceVariant.copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
