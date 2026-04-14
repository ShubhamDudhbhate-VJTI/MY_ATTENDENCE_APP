package com.example.dbms_shubham_application.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.Classroom
import com.example.dbms_shubham_application.data.model.FacultySessionRecord
import com.example.dbms_shubham_application.data.model.SessionDetailsResponse
import com.example.dbms_shubham_application.data.model.Subject
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Calendar

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val SuccessGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val facultyId = sessionManager.getUserId()?.replace("\"", "")?.replace("'", "") ?: ""

    var sessions by remember { mutableStateOf<List<FacultySessionRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Filters
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var classrooms by remember { mutableStateOf<List<Classroom>>(emptyList()) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedClassroomId by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    
    // Details
    var sessionDetails by remember { mutableStateOf<SessionDetailsResponse?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    fun loadSessions() {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getFacultySessions(
                    facultyId, 
                    selectedSubjectId, 
                    selectedClassroomId, 
                    selectedDate
                )
                if (response.isSuccessful) {
                    sessions = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun loadFilters() {
        scope.launch {
            try {
                val subRes = RetrofitClient.apiService.getFacultySubjects(facultyId)
                if (subRes.isSuccessful) subjects = subRes.body() ?: emptyList()
                
                val roomRes = RetrofitClient.apiService.getClassrooms()
                if (roomRes.isSuccessful) classrooms = roomRes.body() ?: emptyList()
            } catch (e: Exception) {}
        }
    }

    fun loadSessionDetails(sessionId: String) {
        scope.launch {
            isLoadingDetails = true
            showDetailsDialog = true
            try {
                val response = RetrofitClient.apiService.getSessionDetails(sessionId)
                if (response.isSuccessful) {
                    sessionDetails = response.body()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingDetails = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFilters()
        loadSessions()
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Attendance History", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        selectedSubjectId = null
                        selectedClassroomId = null
                        selectedDate = null
                        loadSessions()
                    }) {
                        Icon(Icons.Default.FilterListOff, "Clear Filters", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // Filter Bar
            FilterSection(
                subjects = subjects,
                classrooms = classrooms,
                selectedSubjectId = selectedSubjectId,
                selectedClassroomId = selectedClassroomId,
                selectedDate = selectedDate,
                onSubjectChange = { selectedSubjectId = it; loadSessions() },
                onClassroomChange = { selectedClassroomId = it; loadSessions() },
                onDateClick = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(context, { _, year, month, day ->
                        selectedDate = java.util.Locale.getDefault().let { locale ->
                            String.format(locale, "%04d-%02d-%02d", year, month + 1, day)
                        }
                        loadSessions()
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sessions found matching filters", color = TextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = if (selectedDate == null && selectedSubjectId == null) "Recent Sessions" else "Filtered Results",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(sessions) { session ->
                        SessionHistoryCard(session) {
                            loadSessionDetails(session.session_id)
                        }
                    }
                }
            }
        }
    }

    if (showDetailsDialog) {
        SessionDetailsDialog(
            details = sessionDetails,
            isLoading = isLoadingDetails,
            onDismiss = { showDetailsDialog = false; sessionDetails = null }
        )
    }
}

@Composable
fun FilterSection(
    subjects: List<Subject>,
    classrooms: List<Classroom>,
    selectedSubjectId: String?,
    selectedClassroomId: String?,
    selectedDate: String?,
    onSubjectChange: (String?) -> Unit,
    onClassroomChange: (String?) -> Unit,
    onDateClick: () -> Unit
) {
    var subExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Subject Filter
        Box(modifier = Modifier.weight(1f)) {
            FilterChip(
                selected = selectedSubjectId != null,
                onClick = { subExpanded = true },
                label = { Text(subjects.find { it.id == selectedSubjectId }?.name ?: "Subject", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                    selectedLabelColor = AccentBlue,
                    labelColor = TextMuted
                )
            )
            DropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }, modifier = Modifier.background(CardBg)) {
                DropdownMenuItem(text = { Text("All Subjects", color = TextWhite) }, onClick = { onSubjectChange(null); subExpanded = false })
                subjects.forEach { sub ->
                    DropdownMenuItem(text = { Text(sub.name, color = TextWhite) }, onClick = { onSubjectChange(sub.id); subExpanded = false })
                }
            }
        }

        // Room Filter
        Box(modifier = Modifier.weight(1f)) {
            FilterChip(
                selected = selectedClassroomId != null,
                onClick = { roomExpanded = true },
                label = { Text(classrooms.find { it.id == selectedClassroomId }?.name ?: "Room", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                    selectedLabelColor = AccentBlue,
                    labelColor = TextMuted
                )
            )
            DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }, modifier = Modifier.background(CardBg)) {
                DropdownMenuItem(text = { Text("All Rooms", color = TextWhite) }, onClick = { onClassroomChange(null); roomExpanded = false })
                classrooms.forEach { room ->
                    DropdownMenuItem(text = { Text(room.name, color = TextWhite) }, onClick = { onClassroomChange(room.id); roomExpanded = false })
                }
            }
        }

        // Date Filter
        FilterChip(
            modifier = Modifier.weight(1f),
            selected = selectedDate != null,
            onClick = onDateClick,
            label = { Text(selectedDate ?: "Date", fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                selectedLabelColor = AccentBlue,
                labelColor = TextMuted
            )
        )
    }
}

@Composable
fun SessionHistoryCard(session: FacultySessionRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(AccentBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, null, tint = AccentBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.subject_name.ifEmpty { session.subject_id }, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(session.start_time.substring(0, 10), color = TextMuted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.student_count}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Students", color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun SessionDetailsDialog(details: SessionDetailsResponse?, isLoading: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBg,
        title = { Text(details?.subject_name ?: "Session Details", color = TextWhite) },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (details == null) {
                Text("Error loading details", color = Color.Red)
            } else {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    Text("Time: ${details.start_time.replace("T", " ").substring(0, 16)}", color = TextMuted, fontSize = 12.sp)
                    Text("Total Students: ${details.total_students}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(details.students) { student ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(8.dp)).padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(student.student_name, color = TextWhite, fontWeight = FontWeight.Medium)
                                    Text(student.student_id, color = TextMuted, fontSize = 10.sp)
                                }
                                Text(student.marked_at.substring(11, 16), color = AccentBlue, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = AccentBlue) }
        }
    )
}
