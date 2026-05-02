package com.example.dbms_shubham_application.screens

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.dbms_shubham_application.data.model.Subject
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.components.ModernAttendanceCard
import com.example.dbms_shubham_application.utils.DateTimeUtils
import com.example.dbms_shubham_application.utils.PredictiveAttendanceUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val userId = remember { sessionManager.getUserId() ?: "" }
    
    val colorScheme = MaterialTheme.colorScheme
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var attendanceRecords by remember { mutableStateOf<List<com.example.dbms_shubham_application.data.model.AttendanceRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    val fetchAttendance = {
        scope.launch {
            isRefreshing = true
            try {
                Log.d("AttendanceHistory", "Fetching history for userId: $userId")
                val response = RetrofitClient.apiService.getAttendanceHistory(userId)
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    Log.d("AttendanceHistory", "Received ${body.size} records")
                    // Filter out test/fake sessions (cloud___)
                    attendanceRecords = body.filter { !it.session_id.startsWith("cloud___") }
                        .sortedByDescending { it.timestamp }
                } else {
                    Log.e("AttendanceHistory", "Response Error: ${response.code()} ${response.message()}")
                }
                
                // Fetch subjects for filter
                val subResponse = RetrofitClient.apiService.getSubjects()
                if (subResponse.isSuccessful) {
                    subjects = subResponse.body() ?: emptyList()
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
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -20 }, animationSpec = tween(600))
                    ) {
                        Column {
                            Text("Academic Audit", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Text("Your attendance journey", color = colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
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
                        onClick = { 
                            searchQuery = ""
                            selectedSubjectId = null
                            selectedDate = null
                            selectedStatus = null
                            fetchAttendance() 
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .background(colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.FilterListOff, "Reset", tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValue ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValue)) {
            // Search Bar
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 100)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, 100))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    placeholder = { Text("Search by subject...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface
                    )
                )
            }

            // Filter Bar
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 200)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, 200))
            ) {
                StudentFilterSection(
                    subjects = subjects,
                    selectedSubjectId = selectedSubjectId,
                    selectedDate = selectedDate,
                    selectedStatus = selectedStatus,
                    onSubjectChange = { selectedSubjectId = it },
                    onStatusChange = { selectedStatus = it },
                    onDateClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, year, month, day ->
                            selectedDate = java.util.Locale.getDefault().let { locale ->
                                String.format(locale, "%04d-%02d-%02d", year, month + 1, day)
                            }
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { fetchAttendance() },
                modifier = Modifier.fillMaxSize()
            ) {
                val filteredRecords = attendanceRecords.filter { record ->
                    val matchesSearch = try {
                        (record.subject_name?.contains(searchQuery, ignoreCase = true) ?: false) || 
                        (record.subject_id?.contains(searchQuery, ignoreCase = true) ?: false)
                    } catch (e: Exception) { false }
                    
                    val matchesSubject = selectedSubjectId == null || record.subject_id == selectedSubjectId
                    val matchesDate = try {
                        selectedDate == null || record.timestamp?.startsWith(selectedDate!!) == true
                    } catch (e: Exception) { false }
                    
                    val matchesStatus = try {
                        selectedStatus == null || record.status?.equals(selectedStatus, ignoreCase = true) == true
                    } catch (e: Exception) { false }
                    
                    matchesSearch && matchesSubject && matchesDate && matchesStatus
                }

                if (isLoading && attendanceRecords.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorScheme.primary)
                    }
                } else if (filteredRecords.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if (searchQuery.isNotEmpty() || selectedSubjectId != null || selectedDate != null) Icons.Default.FilterList else Icons.Default.EventBusy, 
                                null, tint = colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(120.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isNotEmpty() || selectedSubjectId != null || selectedDate != null) "No records match filters" else "No attendance records found", 
                                color = colorScheme.onSurfaceVariant, 
                                fontWeight = FontWeight.Bold
                            )
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
                            val total = filteredRecords.size
                            val present = filteredRecords.count { it.status.lowercase() == "present" }
                            val absent = total - present
                            val percentage = if (total > 0) (present.toFloat() / total) else 0f
                            
                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(600, 100)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(600, 100))
                            ) {
                                Column {
                                    ModernHistoryStatsCard(percentage, present, absent, total)
                                    
                                    if (selectedSubjectId == null && selectedDate == null && searchQuery.isEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        PredictiveAlertCard(present, total)
                                    }
                                }
                            }
                        }
                        
                        item {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(600, 200))
                            ) {
                                Text(
                                    text = if (searchQuery.isNotEmpty() || selectedSubjectId != null || selectedDate != null) "Filtered Records" else "Attendance Records",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        
                        // Attendance Records
                        items(filteredRecords, key = { it.session_id + "_" + it.timestamp + "_" + it.status }) { record ->
                            val index = filteredRecords.indexOf(record)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(600, 300 + (index * 50))) + 
                                        slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600, 300 + (index * 50)))
                            ) {
                                ModernAttendanceCard(record = record)
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentFilterSection(
    subjects: List<Subject>,
    selectedSubjectId: String?,
    selectedDate: String?,
    selectedStatus: String?,
    onSubjectChange: (String?) -> Unit,
    onStatusChange: (String?) -> Unit,
    onDateClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var subExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Subject Filter
        Box(modifier = Modifier.weight(1.2f)) {
            Surface(
                onClick = { subExpanded = true },
                shape = RoundedCornerShape(14.dp),
                color = if (selectedSubjectId != null) colorScheme.primary else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (selectedSubjectId != null) colorScheme.primary else colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.height(44.dp).fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        subjects.find { it.id == selectedSubjectId }?.name ?: "Subject",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSubjectId != null) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = if (selectedSubjectId != null) colorScheme.onPrimary else colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }, modifier = Modifier.background(colorScheme.surface)) {
                DropdownMenuItem(text = { Text("All Subjects") }, onClick = { onSubjectChange(null); subExpanded = false })
                subjects.forEach { sub ->
                    DropdownMenuItem(text = { Text(sub.name) }, onClick = { onSubjectChange(sub.id); subExpanded = false })
                }
            }
        }

        // Status Filter
        Box(modifier = Modifier.weight(0.8f)) {
            Surface(
                onClick = { statusExpanded = true },
                shape = RoundedCornerShape(14.dp),
                color = if (selectedStatus != null) colorScheme.secondary else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (selectedStatus != null) colorScheme.secondary else colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.height(44.dp).fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        selectedStatus ?: "Status",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedStatus != null) colorScheme.onSecondary else colorScheme.onSurfaceVariant
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = if (selectedStatus != null) colorScheme.onSecondary else colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }, modifier = Modifier.background(colorScheme.surface)) {
                DropdownMenuItem(text = { Text("All") }, onClick = { onStatusChange(null); statusExpanded = false })
                DropdownMenuItem(text = { Text("Present") }, onClick = { onStatusChange("Present"); statusExpanded = false })
                DropdownMenuItem(text = { Text("Absent") }, onClick = { onStatusChange("Absent"); statusExpanded = false })
            }
        }

        // Date Filter
        Surface(
            onClick = onDateClick,
            shape = RoundedCornerShape(14.dp),
            color = if (selectedDate != null) Color(0xFF4CAF50) else colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (selectedDate != null) Color(0xFF4CAF50) else colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.height(44.dp).weight(0.9f)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.CalendarToday, null, tint = if (selectedDate != null) Color.White else colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    selectedDate?.substring(5) ?: "Date",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedDate != null) Color.White else colorScheme.onSurfaceVariant
                )
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
