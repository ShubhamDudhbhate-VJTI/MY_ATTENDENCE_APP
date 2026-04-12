package com.example.dbms_shubham_application.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val SuccessGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyClassesScreen(navController: NavController) {
    var showSessionModal by remember { mutableStateOf(false) }
    var activeSession by remember { mutableStateOf<FacultyClass?>(null) }
    
    val classes = remember {
        listOf(
            FacultyClass("SE-IT Batch A", "Database Management Systems", "R-301", "45 Students"),
            FacultyClass("TE-IT Batch B", "Software Engineering", "R-402", "38 Students"),
            FacultyClass("BE-IT Batch A", "Data Warehousing", "R-105", "52 Students")
        )
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Faculty Portal", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Text("Select Class to Start Session", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(classes) { item ->
                    ClassCard(item) {
                        activeSession = item
                        showSessionModal = true
                    }
                }
            }
        }
    }

    if (showSessionModal && activeSession != null) {
        SessionManagementDialog(
            activeClass = activeSession!!,
            onDismiss = { showSessionModal = false }
        )
    }
}

@Composable
fun ClassCard(item: FacultyClass, onStart: () -> Unit) {
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
                Text(item.batch, color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(item.students, color = TextMuted, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.subject, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Attendance Session")
            }
        }
    }
}

@Composable
fun SessionManagementDialog(activeClass: FacultyClass, onDismiss: () -> Unit) {
    var sessionStatus by remember { mutableStateOf("Initializing...") }
    var timeLeft by remember { mutableStateOf(600) } // 10 minutes in seconds
    
    LaunchedEffect(Unit) {
        delay(1000)
        sessionStatus = "Session Active"
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Active Session", color = TextWhite) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(activeClass.subject, color = AccentBlue, fontWeight = FontWeight.Bold)
                Text(activeClass.batch, color = TextMuted)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Dynamic QR Placeholder
                Box(
                    modifier = Modifier.size(180.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCode, null, tint = Color.Black, modifier = Modifier.size(140.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WiFi Beacon: Broadcast Active", color = SuccessGreen, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Session expires in: ${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("End Session")
            }
        }
    )
}

data class FacultyClass(
    val batch: String,
    val subject: String,
    val room: String,
    val students: String
)
