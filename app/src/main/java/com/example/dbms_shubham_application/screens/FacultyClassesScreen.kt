package com.example.dbms_shubham_application.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- THEME CONSISTENCY REMOVED LEGACY COLORS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyClassesScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val facultyId = sessionManager.getUserId()?.replace("\"", "")?.replace("'", "") ?: ""

    val colorScheme = MaterialTheme.colorScheme

    var schedule by remember { mutableStateOf<List<ScheduleRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var classrooms by remember { mutableStateOf<List<Classroom>>(emptyList()) }
    var editingRecord by remember { mutableStateOf<ScheduleRecord?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    var selectedDay by remember { 
        mutableStateOf(SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()))
    }

    fun loadSchedule(day: String) {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getFacultySchedule(facultyId, day)
                if (response.isSuccessful) {
                    schedule = response.body() ?: emptyList()
                }

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

    LaunchedEffect(selectedDay) {
        loadSchedule(selectedDay)
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(colorScheme.primary.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            ) {
                TopAppBar(
                    title = { 
                        Column {
                            Text("My Schedule", color = colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(selectedDay, color = colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(40.dp)
                                .background(colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                                .border(1.dp, colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorScheme.onBackground, modifier = Modifier.size(20.dp))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .background(colorScheme.secondary.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, colorScheme.secondary.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, "Add Schedule", tint = colorScheme.secondary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { loadSchedule(selectedDay) },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .background(colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Day Selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(days) { day ->
                    val isSelected = selectedDay == day
                    Surface(
                        onClick = { selectedDay = day },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) colorScheme.primary else colorScheme.surface.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isSelected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            } else if (schedule.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, null, tint = colorScheme.onBackground.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No classes scheduled for $selectedDay", color = colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(schedule) { item ->
                        ScheduleCard(
                            item = item,
                            onStart = {
                                val route = "start_session?subject_id=${item.subject_id}&classroom_id=${item.classroom_id}&subject_name=${item.subject}&room_name=${item.room}"
                                navController.navigate(route)
                            },
                            onEdit = { editingRecord = item },
                            onDelete = {
                                scope.launch {
                                    item.id?.let { id ->
                                        val res = RetrofitClient.apiService.deleteScheduleRecord(id)
                                        if (res.isSuccessful) loadSchedule(selectedDay)
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
                            loadSchedule(selectedDay)
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
fun ScheduleCard(item: ScheduleRecord, onStart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.time, color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(item.room, color = colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(item.subject, color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            
            if (!item.subject_code.isNullOrEmpty()) {
                Text(item.subject_code, color = colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            
            if (!item.branch.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Group, null, tint = colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${item.branch} • ${item.year}", color = colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start This Session", fontWeight = FontWeight.Bold)
            }
        }
    }
}
