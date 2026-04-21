package com.example.dbms_shubham_application.screens

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
import com.example.dbms_shubham_application.ui.components.EditScheduleDialog
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val facultyId = sessionManager.getUserId()?.replace("\"", "")?.replace("'", "") ?: ""

    val colorScheme = MaterialTheme.colorScheme

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
                val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                val schedRes = RetrofitClient.apiService.getFacultySchedule(facultyId, currentDay)
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
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Manage Schedule", color = colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Configure your class template", color = colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Normal)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = colorScheme.primary)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                                val res = RetrofitClient.apiService.syncOfficialSchedule(facultyId, currentDay)
                                if (res.isSuccessful) {
                                    val body = res.body()
                                    schedule = body?.schedule ?: emptyList()
                                    val syncInfo = "${body?.day}, ${body?.date}"
                                    Toast.makeText(context, "Synced Official Template: $syncInfo", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Sync failed: ${res.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Icon(Icons.Default.CloudDownload, "Sync Official Template", tint = colorScheme.secondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            } else if (schedule.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CalendarToday, null, tint = colorScheme.onBackground.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No records found for today", color = colorScheme.onBackground.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
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
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.day.uppercase(),
                        color = colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    if (record.is_official) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Official",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(record.subject, color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(record.time, color = colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Place, null, tint = colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(record.room, color = colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            
            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.background(colorScheme.onSurface.copy(alpha = 0.05f), CircleShape).size(36.dp)
                ) {
                    Icon(Icons.Default.Edit, "Edit", tint = colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(colorScheme.error.copy(alpha = 0.1f), CircleShape).size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}


