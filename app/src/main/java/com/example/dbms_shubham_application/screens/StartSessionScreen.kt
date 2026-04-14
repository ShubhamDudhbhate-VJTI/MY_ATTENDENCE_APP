package com.example.dbms_shubham_application.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.*
import com.example.dbms_shubham_application.network.RetrofitClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val SuccessGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionScreen(
    navController: NavController,
    prefillSubjectId: String? = null,
    prefillClassroomId: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    var timeLeft by remember { mutableIntStateOf(180) } // 3 minutes in seconds
    var showReport by remember { mutableStateOf(false) }
    var sessionReport by remember { mutableStateOf<SessionReportResponse?>(null) }

    var classroomExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    var isLoadingInfo by remember { mutableStateOf(false) }

    fun loadInitialData() {
        scope.launch {
            try {
                isLoadingInfo = true
                val sessionManager = SessionManager(context)
                val rawId = sessionManager.getUserId() ?: ""
                val facultyId = rawId.replace("\"", "").replace("'", "").trim()

                Log.d("StartSession", "Fetching data for clean facultyId: '$facultyId'")

                // 1. Fetch Classrooms
                val roomRes = RetrofitClient.apiService.getClassrooms()
                if (roomRes.isSuccessful) {
                    val classroomsData = roomRes.body() ?: emptyList()
                    classrooms = classroomsData
                    // Use prefill if available
                    selectedClassroom = classroomsData.find { it.id == prefillClassroomId } ?: classroomsData.firstOrNull()
                    Log.d("StartSession", "Loaded ${classroomsData.size} classrooms")
                }

                // 2. Fetch Subjects
                val subjectRes = if (facultyId.isNotEmpty()) {
                    RetrofitClient.apiService.getFacultySubjects(facultyId)
                } else {
                    RetrofitClient.apiService.getSubjects()
                }

                if (subjectRes.isSuccessful) {
                    val fetchedSubjects = subjectRes.body() ?: emptyList()
                    subjects = fetchedSubjects
                    selectedSubject = subjects.find { it.id == prefillSubjectId } ?: subjects.firstOrNull()
                } else {
                    Log.e("StartSession", "Subject fetch error: ${subjectRes.code()}")
                    // Fallback
                    val allRes = RetrofitClient.apiService.getSubjects()
                    if (allRes.isSuccessful) {
                        subjects = allRes.body() ?: emptyList()
                        selectedSubject = subjects.find { it.id == prefillSubjectId } ?: subjects.firstOrNull()
                    }
                }

                if (classrooms.isEmpty() && subjects.isEmpty()) {
                    Toast.makeText(context, "No data returned. Check DB or Faculty Assignment.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("StartSession", "Fetch failed", e)
                Toast.makeText(context, "Connection Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isLoadingInfo = false
            }
        }
    }

    // Fetch rooms and subjects on load
    LaunchedEffect(Unit) {
        loadInitialData()
    }

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
                fetchAttendance()
                delay(1000)
                timeLeft--
            }
            if (timeLeft <= 0 && sessionStarted) {
                endSessionOnServer()
            }
        }
    }

    fun startSessionOnServer() {
        if (selectedClassroom == null || selectedSubject == null) {
            Toast.makeText(context, "Select classroom and subject first", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionManager = SessionManager(context)
        val facultyId = sessionManager.getUserId() ?: "UNKNOWN_FACULTY"

        isStarting = true
        scope.launch {
            try {
                val request = StartSessionRequest(
                    faculty_id = facultyId,
                    subject_id = selectedSubject!!.id,
                    classroom_id = selectedClassroom!!.id,
                    duration_minutes = 3 // Standardize to 3 mins for security
                )
                
                val response = RetrofitClient.apiService.startSession(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionId = body.session_id
                    classroomDisplay = body.classroom_name
                    // Encode session_id in the QR for easier parsing by student
                    val qrContent = "${body.session_id}|${body.qr_token}"
                    qrBitmap = generateQRCode(qrContent)
                    sessionStarted = true
                    showReport = false
                } else {
                    Toast.makeText(context, "Failed to start session", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("StartSession", "Error", e)
                Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isStarting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showReport) "Session Summary" else "Start Attendance Session", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (showReport) showReport = false
                        else navController.navigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showReport && sessionReport != null) {
                SessionSummaryView(sessionReport!!) { 
                    showReport = false
                    sessionStarted = false
                    sessionId = ""
                }
            } else if (!sessionStarted) {
                if (isLoadingInfo) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue)
                    }
                } else if (classrooms.isEmpty() && subjects.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No data available", color = TextWhite, fontSize = 18.sp)
                            Text("Check your server connection or IP", color = TextMuted, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { loadInitialData() }) {
                                Text("Retry Fetching")
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Session Details", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Classroom Dropdown
                            ExposedDropdownMenuBox(
                                expanded = classroomExpanded,
                                onExpandedChange = { classroomExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedClassroom?.name ?: "No Classrooms Found",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Classroom") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classroomExpanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = TextMuted,
                                        focusedLabelColor = AccentBlue,
                                        unfocusedLabelColor = TextMuted
                                    ),
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                if (classrooms.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = classroomExpanded,
                                        onDismissRequest = { classroomExpanded = false },
                                        modifier = Modifier.background(CardBg)
                                    ) {
                                        classrooms.forEach { room ->
                                            DropdownMenuItem(
                                                text = { Text(room.name, color = TextWhite) },
                                                onClick = {
                                                    selectedClassroom = room
                                                    classroomExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Subject Dropdown
                            ExposedDropdownMenuBox(
                                expanded = subjectExpanded,
                                onExpandedChange = { subjectExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedSubject?.name ?: "No Subjects Found",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Subject") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = TextMuted,
                                        focusedLabelColor = AccentBlue,
                                        unfocusedLabelColor = TextMuted
                                    ),
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                if (subjects.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = subjectExpanded,
                                        onDismissRequest = { subjectExpanded = false },
                                        modifier = Modifier.background(CardBg)
                                    ) {
                                        subjects.forEach { subject ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(subject.name, color = TextWhite)
                                                        if (!subject.code.isNullOrEmpty()) {
                                                            Text(subject.code, color = TextMuted, fontSize = 12.sp)
                                                        }
                                                        if (!subject.branch.isNullOrEmpty()) {
                                                            Text("${subject.branch} - ${subject.year}", color = AccentBlue, fontSize = 10.sp)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectedSubject = subject
                                                    subjectExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isStarting) {
                        CircularProgressIndicator(color = AccentBlue)
                    } else {
                        Button(
                            enabled = selectedClassroom != null && selectedSubject != null,
                            onClick = { startSessionOnServer() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Initialize Session & QR", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    "Scan to Mark Attendance",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${selectedSubject?.id} - $classroomDisplay",
                    color = SuccessGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Timer Display
                val minutes = timeLeft / 60
                val seconds = timeLeft % 60
                Text(
                    text = String.format("Expiring in %02d:%02d", minutes, seconds),
                    color = if (timeLeft < 30) Color.Red else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Session QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Session ID", color = TextMuted, fontSize = 12.sp)
                        Text(sessionId, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Live Attendance List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Live Attendance (${attendanceList.size})",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = { fetchAttendance() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = AccentBlue)
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (attendanceList.isEmpty()) {
                        Text(
                            "Waiting for students to join...",
                            color = TextMuted,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(attendanceList) { log ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = log.student_name ?: "Unknown Name",
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = log.student_id,
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (log.face_verified) {
                                                Icon(
                                                    Icons.Default.Face,
                                                    contentDescription = "Face Verified",
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(log.status, color = SuccessGreen, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { endSessionOnServer() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("End Session", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SessionSummaryView(report: SessionReportResponse, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Session Ended", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Course: ${report.course_id}", color = TextMuted)

                Divider(modifier = Modifier.padding(vertical = 16.dp), color = TextMuted.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(report.total_present.toString(), color = AccentBlue, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Present", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Student List", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(report.students) { student ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(student.name, color = TextWhite, fontWeight = FontWeight.Bold)
                            Text(student.id, color = TextMuted, fontSize = 12.sp)
                        }
                        Text(
                            student.time.takeLast(8), // Just HH:MM:SS
                            color = SuccessGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back to Dashboard")
        }
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
