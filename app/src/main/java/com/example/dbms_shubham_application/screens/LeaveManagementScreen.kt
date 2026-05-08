package com.example.dbms_shubham_application.screens

import android.app.DatePickerDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.LeaveRequestRecord
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.theme.StatusAbsent
import com.example.dbms_shubham_application.ui.theme.StatusPresent
import com.example.dbms_shubham_application.ui.theme.WarningYellow
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveManagementScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val userId = remember { sessionManager.getUserId() ?: "" }
    
    var showRequestDialog by remember { mutableStateOf(false) }
    var leaveHistory by remember { mutableStateOf<List<LeaveRequestRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val fetchHistory = {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getLeaveHistory(userId)
                if (response.isSuccessful) {
                    leaveHistory = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { showRequestDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Request Leave") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (leaveHistory.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("No leave requests found", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(leaveHistory) { leave ->
                        LeaveRecordCard(leave)
                    }
                }
            }
        }

        if (showRequestDialog) {
            LeaveRequestDialog(
                onDismiss = { showRequestDialog = false },
                onSubmit = { type, start, end, reason ->
                    scope.launch {
                        try {
                            val request = mapOf(
                                "student_id" to userId,
                                "type" to type,
                                "start_date" to start,
                                "end_date" to end,
                                "reason" to reason
                            )
                            val response = RetrofitClient.apiService.submitLeaveRequest(request)
                            if (response.isSuccessful) {
                                fetchHistory()
                                showRequestDialog = false
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun LeaveRecordCard(leave: LeaveRequestRecord) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = when (leave.status.lowercase()) {
        "approved" -> StatusPresent
        "rejected" -> StatusAbsent
        else -> WarningYellow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (leave.type.lowercase()) {
                        "medical" -> Icons.Default.MedicalServices
                        "od" -> Icons.Default.EventAvailable
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = statusColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(leave.type, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${leave.start_date} - ${leave.end_date}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Text(leave.reason, fontSize = 14.sp, maxLines = 1)
            }
            
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    leave.status.uppercase(),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun LeaveRequestDialog(onDismiss: () -> Unit, onSubmit: (String, String, String, String) -> Unit) {
    var type by remember { mutableStateOf("OD") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val context = LocalContext.current

    val datePicker = { onDateSelected: (String) -> Unit ->
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected("$year-${month + 1}-$dayOfMonth")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Leave") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("OD", "Medical", "Personal").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { datePicker { startDate = it } },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) }
                )
                
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { datePicker { endDate = it } },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) }
                )
                
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(type, startDate, endDate, reason) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
