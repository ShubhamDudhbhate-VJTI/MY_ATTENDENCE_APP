package com.example.dbms_shubham_application.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.dbms_shubham_application.data.model.ScheduleRecord
import com.example.dbms_shubham_application.data.model.Subject
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val AccentRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val facultyId = sessionManager.getUserId()?.replace("\"", "")?.replace("'", "") ?: ""

    var schedule by remember { mutableStateOf<List<ScheduleRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<ScheduleRecord?>(null) }

    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var classrooms by remember { mutableStateOf<List<Classroom>>(emptyList()) }

    fun loadData() {
        scope.launch {
            isLoading = true
            try {
                val schedRes = RetrofitClient.apiService.getFacultySchedule(facultyId)
                if (schedRes.isSuccessful) schedule = schedRes.body() ?: emptyList()

                val subRes = RetrofitClient.apiService.getFacultySubjects(facultyId)
                if (subRes.isSuccessful) subjects = subRes.body() ?: emptyList()

                val roomRes = RetrofitClient.apiService.getClassrooms()
                if (roomRes.isSuccessful) classrooms = roomRes.body() ?: emptyList()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Manage Schedule", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = AccentBlue) {
                Icon(Icons.Default.Add, "Add", tint = TextWhite)
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(schedule) { record ->
                    ScheduleEditCard(
                        record = record,
                        onEdit = { editingRecord = record },
                        onDelete = {
                            scope.launch {
                                record.id?.let { id ->
                                    val res = RetrofitClient.apiService.deleteScheduleRecord(id)
                                    if (res.isSuccessful) loadData()
                                }
                            }
                        }
                    )
                }
            }
        }

        if (showAddDialog || editingRecord != null) {
            EditScheduleDialog(
                initialRecord = editingRecord,
                subjects = subjects,
                classrooms = classrooms,
                onDismiss = { showAddDialog = false; editingRecord = null },
                onSave = { newRecord ->
                    scope.launch {
                        val res = if (editingRecord == null) {
                            RetrofitClient.apiService.addScheduleRecord(facultyId, newRecord)
                        } else {
                            RetrofitClient.apiService.updateScheduleRecord(editingRecord!!.id!!, newRecord)
                        }
                        if (res.isSuccessful) {
                            loadData()
                            showAddDialog = false
                            editingRecord = null
                        } else {
                            Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ScheduleEditCard(record: ScheduleRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.day, color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(record.subject, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${record.time} • ${record.room}", color = TextMuted, fontSize = 12.sp)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = TextMuted) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = AccentRed) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleDialog(
    initialRecord: ScheduleRecord?,
    subjects: List<Subject>,
    classrooms: List<Classroom>,
    onDismiss: () -> Unit,
    onSave: (ScheduleRecord) -> Unit
) {
    var selectedDay by remember { mutableStateOf(initialRecord?.day ?: "Monday") }
    var selectedSubject by remember { mutableStateOf(subjects.find { it.id == initialRecord?.subject_id } ?: subjects.firstOrNull()) }
    var selectedRoom by remember { mutableStateOf(classrooms.find { it.id == initialRecord?.classroom_id } ?: classrooms.firstOrNull()) }
    var timeStr by remember { mutableStateOf(initialRecord?.time ?: "09:00 AM - 10:00 AM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text(if (initialRecord == null) "Add Schedule" else "Edit Schedule", color = TextWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Day Selector (Simplified)
                val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                var dayExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { dayExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedDay)
                    }
                    DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                        days.forEach { day ->
                            DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = day; dayExpanded = false })
                        }
                    }
                }

                // Subject Selector
                var subExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { subExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedSubject?.name ?: "Select Subject")
                    }
                    DropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(text = { Text(sub.name) }, onClick = { selectedSubject = sub; subExpanded = false })
                        }
                    }
                }

                // Room Selector
                var roomExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { roomExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedRoom?.name ?: "Select Room")
                    }
                    DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                        classrooms.forEach { room ->
                            DropdownMenuItem(text = { Text(room.name) }, onClick = { selectedRoom = room; roomExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = timeStr,
                    onValueChange = { timeStr = it },
                    label = { Text("Time (e.g. 10:00 AM - 11:00 AM)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedSubject != null && selectedRoom != null) {
                    onSave(ScheduleRecord(
                        id = initialRecord?.id,
                        day = selectedDay,
                        subject = selectedSubject!!.name,
                        subject_id = selectedSubject!!.id,
                        subject_code = selectedSubject!!.code,
                        room = selectedRoom!!.name,
                        classroom_id = selectedRoom!!.id,
                        time = timeStr
                    ))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
