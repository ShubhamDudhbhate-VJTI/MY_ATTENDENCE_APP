package com.example.dbms_shubham_application.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(navController: NavController) {
    val attendanceRecords = remember {
        listOf(
            AttendanceRecord("04 Feb", "Database Mgmt Sys", "09:00 AM", "Face Verification", "Present"),
            AttendanceRecord("03 Feb", "Computer Networks", "11:00 AM", "QR Scanner", "Present"),
            AttendanceRecord("02 Feb", "Software Eng.", "02:00 PM", "Face Verification", "Absent"),
            AttendanceRecord("01 Feb", "Machine Learning", "10:00 AM", "Face Verification", "Present"),
            AttendanceRecord("31 Jan", "Database Mgmt Sys", "09:00 AM", "QR Scanner", "Present")
        )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Stats Overview
            item {
                HistoryStatsCard()
            }
            
            item {
                Text(
                    text = "Recent Records",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
            
            // Attendance Records
            items(attendanceRecords) { record ->
                HistoryRecordCard(record = record)
            }
        }
    }
}

@Composable
fun HistoryStatsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Overall Rate", fontSize = 14.sp, color = TextMuted)
                    Text("83.2%", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                }
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                progress = { 0.832f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AccentBlue,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniStatItem("Present", "124", AccentGreen)
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(TextMuted.copy(alpha = 0.2f)))
                MiniStatItem("Absent", "25", AccentRed)
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(TextMuted.copy(alpha = 0.2f)))
                MiniStatItem("Total", "149", TextWhite)
            }
        }
    }
}

@Composable
fun MiniStatItem(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 12.sp, color = TextMuted)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun HistoryRecordCard(record: AttendanceRecord) {
    val isPresent = record.status == "Present"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isPresent) AccentGreen.copy(alpha = 0.1f) else AccentRed.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isPresent) AccentGreen else AccentRed,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(record.subject, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(record.date, fontSize = 12.sp, color = TextMuted)
                    Text(" • ", fontSize = 12.sp, color = TextMuted)
                    Text(record.time, fontSize = 12.sp, color = TextMuted)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = (if (isPresent) AccentGreen else AccentRed).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = record.status,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPresent) AccentGreen else AccentRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(record.method, fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

data class AttendanceRecord(
    val date: String,
    val subject: String,
    val time: String,
    val method: String,
    val status: String
)
