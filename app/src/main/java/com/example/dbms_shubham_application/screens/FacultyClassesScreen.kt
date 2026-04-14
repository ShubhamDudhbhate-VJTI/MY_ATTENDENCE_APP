package com.example.dbms_shubham_application.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.ScheduleRecord
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.delay
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
fun FacultyClassesScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val facultyId = sessionManager.getUserId()?.replace("\"", "")?.replace("'", "") ?: ""

    var schedule by remember { mutableStateOf<List<ScheduleRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    var selectedDay by remember { 
        mutableStateOf(
            when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                else -> "Monday"
            }
        )
    }

    fun loadSchedule(day: String) {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getFacultySchedule(facultyId, day)
                if (response.isSuccessful) {
                    schedule = response.body() ?: emptyList()
                }
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
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("My Schedule", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { loadSchedule(selectedDay) }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Day Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days) { day ->
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = { Text(day) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = TextWhite,
                            containerColor = CardBg,
                            labelColor = TextMuted
                        )
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (schedule.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, null, tint = TextMuted, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No classes scheduled for $selectedDay", color = TextMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(schedule) { item ->
                        ScheduleCard(item) {
                            // Navigate to Start Session with pre-filled room and subject
                            val route = "start_session?subject_id=${item.subject_id}&classroom_id=${item.classroom_id}"
                            navController.navigate(route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(item: ScheduleRecord, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.time, color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier.background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(item.room, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(item.subject, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            
            if (!item.subject_code.isNullOrEmpty()) {
                Text(item.subject_code, color = TextMuted, fontSize = 12.sp)
            }
            
            if (!item.branch.isNullOrEmpty()) {
                Text("${item.branch} • ${item.year}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start This Session")
            }
        }
    }
}
