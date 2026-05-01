package com.example.dbms_shubham_application.screens

import android.app.DatePickerDialog
import android.widget.Toast
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
import com.example.dbms_shubham_application.data.model.Classroom
import com.example.dbms_shubham_application.data.model.FacultySessionRecord
import com.example.dbms_shubham_application.data.model.SessionDetailsResponse
import com.example.dbms_shubham_application.data.model.Subject
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.components.ModernDetailsDialog
import com.example.dbms_shubham_application.ui.components.ModernReportCard
import com.example.dbms_shubham_application.utils.DateTimeUtils
import com.example.dbms_shubham_application.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val facultyId = sessionManager.getUserId() ?: ""

    val colorScheme = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current

    var sessions by remember { mutableStateOf<List<FacultySessionRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var classrooms by remember { mutableStateOf<List<Classroom>>(emptyList()) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedClassroomId by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    
    // Details
    var sessionDetails by remember { mutableStateOf<SessionDetailsResponse?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf<String?>(null) }
    var isDownloadingExcel by remember { mutableStateOf<String?>(null) }

    fun loadSessions() {
        scope.launch {
            isRefreshing = true
            try {
                val response = RetrofitClient.apiService.getFacultySessions(
                    facultyId, 
                    selectedSubjectId, 
                    selectedClassroomId, 
                    selectedDate
                )
                if (response.isSuccessful) {
                    // STRICT TEMPORAL SORTING: Absolute newest sessions appear first
                    sessions = (response.body() ?: emptyList()).sortedByDescending { it.start_time }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
                isRefreshing = false
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

    fun downloadPdf(sessionId: String, fileName: String) {
        isDownloading = sessionId
        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.downloadReportPdf(sessionId)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        FileUtils.saveFile(body.byteStream(), fileName, "application/pdf", context)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) { isDownloading = null }
            }
        }
    }

    fun downloadExcel(sessionId: String, fileName: String) {
        isDownloadingExcel = sessionId
        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.downloadReportExcel(sessionId)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        FileUtils.saveFile(body.byteStream(), fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", context)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Excel download failed", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) { isDownloadingExcel = null }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.deleteSession(sessionId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Session deleted successfully", Toast.LENGTH_SHORT).show()
                    loadSessions()
                } else {
                    Toast.makeText(context, "Failed to delete session", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-refresh when returning to this screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadSessions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        loadFilters()
        loadSessions()
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Session Intelligence", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("${sessions.size} academic sessions recorded", color = colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            selectedSubjectId = null
                            selectedClassroomId = null
                            selectedDate = null
                            loadSessions()
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .background(colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.FilterListOff, "Clear", tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = { Text("Search by subject or room...", fontSize = 14.sp) },
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

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { loadSessions() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading && sessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorScheme.primary)
                    }
                } else if (sessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, null, tint = colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(120.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No sessions matched filters", color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val filteredSessions = sessions.filter {
                        it.subject_name.contains(searchQuery, ignoreCase = true) ||
                        it.subject_id.contains(searchQuery, ignoreCase = true) ||
                        it.classroom_id.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredSessions.isEmpty() && searchQuery.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No results for \"$searchQuery\"", color = colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            items(filteredSessions.size) { index ->
                                val session = filteredSessions[index]
                                ModernReportCard(
                                    session = session,
                                    isDownloading = isDownloading == session.session_id || isDownloadingExcel == session.session_id,
                                    isNew = index == 0 && searchQuery.isEmpty(),
                                    onDownload = { 
                                        downloadPdf(
                                            session.session_id, 
                                            "Attendance_${session.subject_name.replace(" ", "_")}_${DateTimeUtils.formatDateOnly(session.start_time)}.pdf"
                                        ) 
                                    },
                                    onDownloadExcel = {
                                        downloadExcel(
                                            session.session_id,
                                            "Attendance_${session.subject_name.replace(" ", "_")}_${DateTimeUtils.formatDateOnly(session.start_time)}.xlsx"
                                        )
                                    },
                                    onClick = { loadSessionDetails(session.session_id) },
                                    onDelete = { deleteSession(session.session_id) }
                                )
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showDetailsDialog) {
        var showManualEntry by remember { mutableStateOf(false) }
        var manualStudentId by remember { mutableStateOf("") }

        ModernDetailsDialog(
            details = sessionDetails,
            isLoading = isLoadingDetails,
            onDismiss = { showDetailsDialog = false; sessionDetails = null },
            onAddManual = { showManualEntry = true }
        )

        if (showManualEntry) {
            AlertDialog(
                onDismissRequest = { showManualEntry = false },
                title = { Text("Manual Attendance") },
                text = {
                    OutlinedTextField(
                        value = manualStudentId,
                        onValueChange = { manualStudentId = it },
                        label = { Text("Enter Student ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                val response = RetrofitClient.apiService.addManualAttendance(mapOf(
                                    "session_id" to (sessionDetails?.session_id ?: ""),
                                    "student_id" to manualStudentId
                                ))
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Attendance added", Toast.LENGTH_SHORT).show()
                                    sessionDetails?.session_id?.let { loadSessionDetails(it) }
                                    showManualEntry = false
                                    manualStudentId = ""
                                }
                            } catch (e: Exception) {}
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showManualEntry = false }) { Text("Cancel") }
                }
            )
        }
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
    val colorScheme = MaterialTheme.colorScheme
    var subExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }

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
                        fontSize = 12.sp,
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

        // Room Filter
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                onClick = { roomExpanded = true },
                shape = RoundedCornerShape(14.dp),
                color = if (selectedClassroomId != null) colorScheme.secondary else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (selectedClassroomId != null) colorScheme.secondary else colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.height(44.dp).fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        classrooms.find { it.id == selectedClassroomId }?.name ?: "Room",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedClassroomId != null) colorScheme.onSecondary else colorScheme.onSurfaceVariant
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = if (selectedClassroomId != null) colorScheme.onSecondary else colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }, modifier = Modifier.background(colorScheme.surface)) {
                DropdownMenuItem(text = { Text("All Rooms") }, onClick = { onClassroomChange(null); roomExpanded = false })
                classrooms.forEach { room ->
                    DropdownMenuItem(text = { Text(room.name) }, onClick = { onClassroomChange(room.id); roomExpanded = false })
                }
            }
        }

        // Date Filter
        Surface(
            onClick = onDateClick,
            shape = RoundedCornerShape(14.dp),
            color = if (selectedDate != null) Color(0xFF4CAF50) else colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (selectedDate != null) Color(0xFF4CAF50) else colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.height(44.dp).weight(1f)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.CalendarToday, null, tint = if (selectedDate != null) Color.White else colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    selectedDate?.substring(5) ?: "Date",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedDate != null) Color.White else colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
