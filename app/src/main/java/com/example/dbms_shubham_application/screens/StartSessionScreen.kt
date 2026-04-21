package com.example.dbms_shubham_application.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.*
import com.example.dbms_shubham_application.network.RetrofitClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                val rawId = sessionManager.getUserId() ?: ""
                val facultyId = rawId.replace("\"", "").replace("'", "").trim()

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
                    Text(
                        if (showReport) "Session Summary" else "New Session", 
                        color = colorScheme.onBackground, 
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (showReport) showReport = false else navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground,
                    navigationIconContentColor = colorScheme.onBackground
                )
            )
        },
        containerColor = colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showReport && sessionReport != null) {
                ModernSessionSummaryView(sessionReport!!) { 
                    showReport = false
                    sessionStarted = false
                    sessionId = ""
                }
            } else if (!sessionStarted) {
                if (isLoadingInfo) {
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
                            // Smart Schedule Matcher
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            try {
                                                val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                                                val facultyId = SessionManager(context).getUserId()?.replace("\"", "")?.replace("'", "")?.trim() ?: ""
                                                val res = RetrofitClient.apiService.getFacultySchedule(facultyId, currentDay)
                                                if (res.isSuccessful) {
                                                    val todaySchedule = res.body() ?: emptyList()
                                                    val now = Calendar.getInstance()
                                                    val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
                                                    
                                                    val currentClass = todaySchedule.find { record ->
                                                        try {
                                                            val times = record.time.split("-")
                                                            if (times.size == 2) {
                                                                val startTime = times[0].trim()
                                                                val endTime = times[1].trim()
                                                                currentTimeStr >= startTime && currentTimeStr <= endTime
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
                                colors = CardDefaults.cardColors(containerColor = colorScheme.primary.copy(alpha = 0.1f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(48.dp).background(colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, null, tint = colorScheme.primary)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("Today's Schedule", color = colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Auto-fill from timetable", color = colorScheme.primary, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, null, tint = colorScheme.primary)
                                }
                            }
                        }

                        item {
                            Text(
                                "Configuration",
                                modifier = Modifier.fillMaxWidth(),
                                color = colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        item {
                            ModernDropdown(
                                label = "Classroom Location",
                                selected = selectedClassroom?.name ?: "Select Classroom",
                                expanded = classroomExpanded,
                                items = classrooms,
                                onExpandedChange = { classroomExpanded = it },
                                onSelect = { selectedClassroom = it; classroomExpanded = false }
                            )
                        }

                        item {
                            ModernDropdown(
                                label = "Teaching Subject",
                                selected = selectedSubject?.name ?: "Select Subject",
                                expanded = subjectExpanded,
                                items = subjects,
                                onExpandedChange = { subjectExpanded = it },
                                onSelect = { selectedSubject = it; subjectExpanded = false }
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
                                    CircularProgressIndicator(color = colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Generate Secure QR", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Active Session View
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ActiveSessionHeader(selectedSubject?.name ?: "Class", classroomDisplay, timeLeft)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Polished QR Container
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

                    // Live Attendance Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Real-time Attendance", color = colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${attendanceList.size} students verified", color = colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        IconButton(
                            onClick = { fetchAttendance() },
                            modifier = Modifier.background(colorScheme.surface, CircleShape).border(1.dp, colorScheme.outline.copy(alpha = 0.2f), CircleShape)
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
                                    Text("Waiting for scans...", color = colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
                                }
                            }
                        } else {
                            items(attendanceList) { log ->
                                ModernAttendanceItem(log)
                            }
                        }
                    }
                }

                // Bottom Action Button
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
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                    ) {
                        Text("Finish Session", fontWeight = FontWeight.Black)
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
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.student_name ?: "Unknown Student", color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(log.student_id, color = colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            if (log.face_verified) {
                Icon(Icons.Default.Verified, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
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
    onSelect: (T) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = colorScheme.onSurface.copy(alpha = 0.6f)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.2f),
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface,
                focusedContainerColor = colorScheme.surface.copy(alpha = 0.5f),
                unfocusedContainerColor = colorScheme.surface.copy(alpha = 0.5f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(colorScheme.surface).border(1.dp, colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        ) {
            items.forEach { item ->
                val text = when(item) {
                    is Classroom -> item.name
                    is Subject -> item.name
                    else -> item.toString()
                }
                DropdownMenuItem(
                    text = { Text(text, color = colorScheme.onSurface, fontWeight = FontWeight.Medium) },
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
fun ModernSessionSummaryView(report: SessionReportResponse, onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            shape = RoundedCornerShape(32.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(80.dp).background(colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = colorScheme.primary, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Session Completed", color = colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(report.course_id, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SummaryStat("Present", report.total_present.toString(), colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Participant List", color = colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth())
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(report.students) { student ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(student.name, color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(student.id, color = colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                    Text(student.time.takeLast(8), color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
        ) {
            Text("Return to Dashboard", fontWeight = FontWeight.ExtraBold, color = colorScheme.onPrimary)
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
