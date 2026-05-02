package com.example.dbms_shubham_application.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.*
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.components.ModernTextField
import com.example.dbms_shubham_application.utils.DateTimeUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*
import com.example.dbms_shubham_application.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionScreen(
    navController: NavController,
    prefillSubjectId: String? = null,
    prefillClassroomId: String? = null,
    prefillSubjectName: String? = null,
    prefillRoomName: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val colorScheme = MaterialTheme.colorScheme

    var classrooms by remember { mutableStateOf<List<Classroom>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var selectedClassroom by remember { mutableStateOf<Classroom?>(null) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    
    var isStarting by remember { mutableStateOf(false) }
    var sessionStarted by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sessionId by remember { mutableStateOf("") }
    var classroomDisplay by remember { mutableStateOf("") }
    var attendanceList by remember { mutableStateOf<List<AttendanceLog>>(emptyList()) }

    var timeLeft by remember { mutableIntStateOf(180) } 
    var showReport by remember { mutableStateOf(false) }
    var sessionReport by remember { mutableStateOf<SessionReportResponse?>(null) }

    var classroomExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var isLoadingInfo by remember { mutableStateOf(false) }

    // --- CONNECTION LOGIC ---
    fun loadInitialData() {
        scope.launch {
            try {
                isLoadingInfo = true
                val sessionManager = SessionManager(context)
                val facultyId = sessionManager.getUserId() ?: ""

                val roomRes = RetrofitClient.apiService.getClassrooms()
                if (roomRes.isSuccessful) {
                    val classroomsData = roomRes.body() ?: emptyList()
                    classrooms = classroomsData
                    selectedClassroom = classroomsData.find { it.id == prefillClassroomId || it.name == prefillRoomName } ?: classroomsData.firstOrNull()
                }

                val subjectRes = if (facultyId.isNotEmpty()) {
                    RetrofitClient.apiService.getFacultySubjects(facultyId)
                } else {
                    RetrofitClient.apiService.getSubjects()
                }

                if (subjectRes.isSuccessful) {
                    val fetchedSubjects = subjectRes.body() ?: emptyList()
                    subjects = fetchedSubjects
                    selectedSubject = subjects.find { it.id == prefillSubjectId || it.name == prefillSubjectName } ?: subjects.firstOrNull()
                } else {
                    val allRes = RetrofitClient.apiService.getSubjects()
                    if (allRes.isSuccessful) {
                        subjects = allRes.body() ?: emptyList()
                        selectedSubject = subjects.find { it.id == prefillSubjectId || it.name == prefillSubjectName } ?: subjects.firstOrNull()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Connection Error", Toast.LENGTH_LONG).show()
            } finally {
                isLoadingInfo = false
            }
        }
    }

    LaunchedEffect(Unit) { loadInitialData() }

    fun fetchAttendance() {
        if (sessionId.isEmpty()) return
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getLiveAttendance(sessionId)
                if (response.isSuccessful && response.body() != null) {
                    attendanceList = response.body()!!.students
                }
            } catch (e: Exception) {
                Log.e("StartSession", "Error fetching attendance", e)
            }
        }
    }

    fun endSessionOnServer() {
        if (sessionId.isEmpty()) return
        scope.launch {
            try {
                val response = RetrofitClient.apiService.stopSession(sessionId)
                if (response.isSuccessful && response.body() != null) {
                    sessionReport = response.body()
                    showReport = true
                    sessionStarted = false
                }
            } catch (e: Exception) {
                Log.e("StartSession", "Error stopping session", e)
            }
        }
    }

    fun downloadSessionReport(sid: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.downloadReportPdf(sid)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        FileUtils.saveFile(body.byteStream(), "Report_$sid.pdf", "application/pdf", context)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(sessionStarted, sessionId) {
        if (sessionStarted && sessionId.isNotEmpty()) {
            timeLeft = 180
            while (sessionStarted && timeLeft > 0) {
                if (timeLeft % 5 == 0) fetchAttendance()
                delay(1000)
                timeLeft--
            }
            if (timeLeft <= 0 && sessionStarted) endSessionOnServer()
        }
    }

    fun startSessionOnServer() {
        if (selectedClassroom == null || selectedSubject == null) {
            Toast.makeText(context, "Selections required", Toast.LENGTH_SHORT).show()
            return
        }
        val facultyId = SessionManager(context).getUserId() ?: "UNKNOWN"
        isStarting = true
        scope.launch {
            try {
                val request = StartSessionRequest(
                    faculty_id = facultyId,
                    subject_id = selectedSubject!!.id,
                    classroom_id = selectedClassroom!!.id,
                    duration_minutes = 3
                )
                val response = RetrofitClient.apiService.startSession(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionId = body.session_id
                    classroomDisplay = body.classroom_name
                    qrBitmap = generateQRCode("${body.session_id}|${body.qr_token}")
                    sessionStarted = true
                    showReport = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to start session", Toast.LENGTH_LONG).show()
            } finally {
                isStarting = false
            }
        }
    }

    // --- UI RENDERING ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (showReport) "Session Summary" else "Live Audit", 
                            color = colorScheme.onBackground, 
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        ) 
                        if (!showReport && !sessionStarted) {
                            Text("Initialize a new classroom log", color = colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (showReport) showReport = false else navController.navigateUp() },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                            .background(colorScheme.surface, CircleShape)
                            .border(1.dp, colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = colorScheme.onBackground,
                    navigationIconContentColor = colorScheme.onBackground
                )
            )
        },
        containerColor = colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = when {
                    showReport && sessionReport != null -> 2
                    sessionStarted -> 1
                    else -> 0
                },
                transitionSpec = {
                    fadeIn(tween(600)) togetherWith fadeOut(tween(600))
                },
                label = "session_state_transition"
            ) { state ->
                when (state) {
                    2 -> ModernSessionSummaryView(
                        report = sessionReport!!,
                        onDownload = { downloadSessionReport(sessionReport!!.session_id) },
                        onDismiss = {
                            showReport = false
                            sessionStarted = false
                            sessionId = ""
                            navController.navigate("reports") {
                                popUpTo("dashboard") { inclusive = false }
                            }
                        }
                    )
                    1 -> Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ActiveSessionHeader(selectedSubject?.name ?: "Class", classroomDisplay, timeLeft)
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = colorScheme.primary)
                                .background(Color.White, RoundedCornerShape(32.dp))
                                .border(8.dp, Color.White, RoundedCornerShape(32.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            qrBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "QR",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Real-time Intelligence", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("${attendanceList.size} students verified", color = colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { fetchAttendance() },
                                modifier = Modifier.background(colorScheme.surface, CircleShape).border(1.dp, colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Refresh, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            if (attendanceList.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Text("Waiting for authentication scans...", color = colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                items(attendanceList, key = { it.student_id }) { log ->
                                    ModernAttendanceItem(log)
                                }
                            }
                        }
                    }
                    0 -> if (isLoadingInfo) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colorScheme.primary, strokeWidth = 3.dp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(vertical = 20.dp)
                        ) {
                            item {
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { visible = true }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 4 }
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    try {
                                                        val currentDay = DateTimeUtils.getCurrentDayName()
                                                        val facultyId = SessionManager(context).getUserId() ?: ""
                                                        val res = RetrofitClient.apiService.getFacultySchedule(facultyId, currentDay)
                                                        if (res.isSuccessful) {
                                                            val todaySchedule = res.body() ?: emptyList()
                                                            val currentMinutesSinceMidnight = DateTimeUtils.getCurrentMinutesSinceMidnight()
                                                            
                                                            val currentClass = todaySchedule.find { record ->
                                                                try {
                                                                    val times = record.time.split("-")
                                                                    if (times.size == 2) {
                                                                        val startMins = DateTimeUtils.parseTimeToMinutes(times[0])
                                                                        val endMins = DateTimeUtils.parseTimeToMinutes(times[1])
                                                                        
                                                                        currentMinutesSinceMidnight in startMins..endMins
                                                                    } else false
                                                                } catch (e: Exception) { false }
                                                            } ?: todaySchedule.firstOrNull()

                                                            currentClass?.let { record ->
                                                                selectedClassroom = classrooms.find { it.id == record.classroom_id || it.name == record.room }
                                                                selectedSubject = subjects.find { it.id == record.subject_id || it.name == record.subject }
                                                                Toast.makeText(context, "Matched: ${record.subject} in ${record.room}", Toast.LENGTH_SHORT).show()
                                                            } ?: Toast.makeText(context, "No active class found in schedule", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Failed to sync schedule", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = colorScheme.primary.copy(alpha = 0.08f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(48.dp).background(colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.AutoFixHigh, null, tint = colorScheme.primary, modifier = Modifier.size(24.dp))
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Column {
                                                Text("Academic Context", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                                Text("Sync with timetable", color = colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.weight(1f))
                                            Icon(Icons.Default.ChevronRight, null, tint = colorScheme.primary.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "Configuration",
                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                                    color = colorScheme.onBackground,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }

                            item {
                                ModernDropdown(
                                    label = "Classroom Location",
                                    selected = selectedClassroom?.name ?: "Select Classroom",
                                    expanded = classroomExpanded,
                                    items = classrooms,
                                    onExpandedChange = { classroomExpanded = it },
                                    onSelect = { selectedClassroom = it; classroomExpanded = false },
                                    icon = Icons.Default.LocationOn
                                )
                            }

                            item {
                                ModernDropdown(
                                    label = "Teaching Subject",
                                    selected = selectedSubject?.name ?: "Select Subject",
                                    expanded = subjectExpanded,
                                    items = subjects,
                                    onExpandedChange = { subjectExpanded = it },
                                    onSelect = { selectedSubject = it; subjectExpanded = false },
                                    icon = Icons.Default.MenuBook
                                )
                            }

                            item {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { startSessionOnServer() },
                                    modifier = Modifier.fillMaxWidth().height(60.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                                    enabled = !isStarting && selectedClassroom != null && selectedSubject != null
                                ) {
                                    if (isStarting) {
                                        CircularProgressIndicator(color = colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                                    } else {
                                        Text("Initialize Secure Channel", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (sessionStarted && !showReport) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, colorScheme.background)))
                        .padding(24.dp)
                ) {
                    Button(
                        onClick = { endSessionOnServer() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                    ) {
                        Text("Terminate & Save Log", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSessionHeader(subject: String, room: String, timeLeft: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val mins = timeLeft / 60
    val secs = timeLeft % 60
    val timerColor = if (timeLeft < 30) colorScheme.error else colorScheme.primary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = timerColor.copy(alpha = 0.1f),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, timerColor.copy(alpha = 0.3f))
        ) {
            Text(
                text = String.format("%02d:%02d", mins, secs),
                color = timerColor,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(subject, color = colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(room, color = colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ModernAttendanceItem(log: AttendanceLog) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.student_name ?: "Unknown Student", color = colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(log.student_id, color = colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (log.face_verified) {
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("VERIFIED", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ModernDropdown(
    label: String,
    selected: String,
    expanded: Boolean,
    items: List<T>,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (T) -> Unit,
    icon: ImageVector
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Box(modifier = Modifier.fillMaxWidth()) {
        ModernTextField(
            value = selected,
            onValueChange = {},
            label = label,
            icon = icon,
            modifier = Modifier.clickable { onExpandedChange(true) },
            colors = colorScheme.primary to colorScheme.outline,
            textColor = colorScheme.onBackground,
            surfaceColor = colorScheme.surface
        )
        // Overlay a transparent box to capture clicks since ModernTextField is not read-only by default in its params
        Box(modifier = Modifier.matchParentSize().clickable { onExpandedChange(true) })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(colorScheme.surface).width(300.dp).border(1.dp, colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            items.forEach { item ->
                val text = when(item) {
                    is Classroom -> item.name
                    is Subject -> item.name
                    else -> item.toString()
                }
                DropdownMenuItem(
                    text = { Text(text, color = colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
fun ModernSessionSummaryView(
    report: SessionReportResponse, 
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isDownloading by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 4 }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                Brush.linearGradient(listOf(colorScheme.primary, colorScheme.secondary)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Session Audited", color = colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(report.course_id ?: "Academic Record", color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryStat("Present", report.total_present.toString(), colorScheme.primary)
                        VerticalDivider(modifier = Modifier.height(40.dp), color = colorScheme.outline.copy(alpha = 0.2f))
                        SummaryStat("Success", "100%", Color(0xFF4CAF50))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = { 
                    isDownloading = true
                    onDownload()
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.primary)
            ) {
                Icon(Icons.Default.FileDownload, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Secure PDF Report", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Participant Intel", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.fillMaxWidth())
            
            var showManualEntry by remember { mutableStateOf(false) }
            var manualStudentId by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showManualEntry = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.primary)
                ) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Manual Entry", fontWeight = FontWeight.Bold)
                }
            }

            if (showManualEntry) {
                AlertDialog(
                    onDismissRequest = { showManualEntry = false },
                    title = { Text("Manual Attendance", fontWeight = FontWeight.Black) },
                    text = {
                        OutlinedTextField(
                            value = manualStudentId,
                            onValueChange = { manualStudentId = it },
                            label = { Text("Student Roll Number") },
                            placeholder = { Text("e.g. 210101") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (manualStudentId.isBlank()) return@Button
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.addManualAttendance(mapOf(
                                        "session_id" to report.session_id,
                                        "student_id" to manualStudentId.trim()
                                    ))
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Attendance added", Toast.LENGTH_SHORT).show()
                                        showManualEntry = false
                                        manualStudentId = ""
                                        // Note: In a real app, we'd ideally refresh the report object here
                                    } else {
                                        Toast.makeText(context, "Failed to add", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) { Text("Add") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showManualEntry = false }) { Text("Cancel") }
                    },
                    shape = RoundedCornerShape(24.dp)
                )
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                val studentList = report.students ?: emptyList()
                if (studentList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No participants recorded", color = colorScheme.onBackground.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    items(studentList, key = { it.id ?: it.name ?: "" }) { student ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .border(1.dp, colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).background(colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                    Text(student.name?.firstOrNull()?.toString() ?: "?", color = colorScheme.primary, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(student.name ?: "Unknown", color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(student.id ?: "---", color = colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(DateTimeUtils.formatTimeOnly(student.time), color = colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) {
                Text("Complete & Return", fontWeight = FontWeight.ExtraBold, color = colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

private fun generateQRCode(text: String): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}
