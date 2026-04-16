package com.example.dbms_shubham_application.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId = sessionManager.getUserId()?.replace("\"", "") ?: ""
    val scope = rememberCoroutineScope()
    
    var reports by remember { mutableStateOf<List<ReportData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            scope.launch {
                try {
                    val response = RetrofitClient.apiService.getFacultySessions(userId)
                    if (response.isSuccessful) {
                        val sessions = response.body() ?: emptyList()
                        reports = sessions.map { session ->
                            ReportData(
                                name = "Attendance: ${session.subject_id}",
                                description = "Date: ${session.start_time.take(10)} | ${session.student_count} Students",
                                format = "PDF",
                                sessionId = session.session_id
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Faculty Reports", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("View and download session logs", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Filter Logic */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (reports.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No session reports available yet.", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    items(reports) { report ->
                        ReportCard(report)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(report: ReportData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AccentBlue.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, AccentBlue.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(report.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(report.description, color = TextMuted, fontSize = 12.sp)
            }
            
            IconButton(
                onClick = { /* Download PDF */ },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = "Download", tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}


data class ReportData(
    val name: String,
    val description: String,
    val format: String,
    val sessionId: String = ""
)
