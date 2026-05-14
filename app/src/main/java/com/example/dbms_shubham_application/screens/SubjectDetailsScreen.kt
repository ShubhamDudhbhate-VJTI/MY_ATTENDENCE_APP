package com.example.dbms_shubham_application.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.AttendanceRecord
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.components.ModernAttendanceCard
import com.example.dbms_shubham_application.ui.theme.StatusAbsent
import com.example.dbms_shubham_application.ui.theme.StatusPresent
import com.example.dbms_shubham_application.ui.theme.WarningYellow
import com.example.dbms_shubham_application.utils.PredictiveAttendanceUtils

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailsScreen(
    navController: NavController,
    subjectId: String,
    subjectName: String,
    percentage: Double,
    attended: Int,
    total: Int
) {
    val decodedId = remember(subjectId) { URLDecoder.decode(subjectId, StandardCharsets.UTF_8.toString()) }
    val decodedName = remember(subjectName) { URLDecoder.decode(subjectName, StandardCharsets.UTF_8.toString()) }
    
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId = remember { sessionManager.getUserId()?.replace("\"", "")?.replace("'", "") ?: "" }
    val colorScheme = MaterialTheme.colorScheme

    var history by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId, decodedId) {
        try {
            val response = RetrofitClient.apiService.getAttendanceHistory(userId)
            if (response.isSuccessful) {
                history = response.body()?.filter {
                    it.subject_id == decodedId && !it.session_id.startsWith("cloud___")
                } ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val filteredHistory = remember(history, selectedStatus) {
        if (selectedStatus == null) history
        else history.filter { it.status.equals(selectedStatus, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colorScheme.primary.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                decodedName,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                decodedId,
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier.padding(8.dp).background(colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(600))
                    ) {
                        SubjectStatsCard(percentage, attended, total)
                    }
                }

                item {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(800)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(800))
                    ) {
                        SubjectPredictiveCard(attended, total)
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Attendance History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModernFilterChip(
                                selected = selectedStatus == null,
                                onClick = { selectedStatus = null },
                                label = "All Logs"
                            )
                            ModernFilterChip(
                                selected = selectedStatus == "Present",
                                onClick = { selectedStatus = "Present" },
                                label = "Present"
                            )
                            ModernFilterChip(
                                selected = selectedStatus == "Absent",
                                onClick = { selectedStatus = "Absent" },
                                label = "Absent"
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                        }
                    }
                } else if (filteredHistory.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("No records found", color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(filteredHistory) { record ->
                        ModernAttendanceCard(record = record)
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun ModernFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (selected) colorScheme.primary else colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.2f)),
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colorScheme.onPrimary else colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun SubjectStatsCard(percentage: Double, attended: Int, total: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val color = when {
        percentage >= 0.75 -> StatusPresent
        percentage >= 0.65 -> WarningYellow
        else -> StatusAbsent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Current Standing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                    Text("${(percentage * 100).toInt()}%", fontSize = 48.sp, fontWeight = FontWeight.Black, color = color)
                }
                
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { percentage.toFloat() },
                        modifier = Modifier.size(80.dp),
                        color = color,
                        strokeWidth = 8.dp,
                        trackColor = color.copy(alpha = 0.1f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Icon(
                        imageVector = if (percentage >= 0.75) Icons.Default.Verified else Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailBox(Modifier.weight(1f), "Attended", "$attended", StatusPresent)
                DetailBox(Modifier.weight(1f), "Absent", "${total - attended}", StatusAbsent)
                DetailBox(Modifier.weight(1f), "Total", "$total", colorScheme.secondary)
            }
        }
    }
}

@Composable
fun SubjectPredictiveCard(attended: Int, total: Int) {
    val target = 0.75
    val current = if (total > 0) attended.toDouble() / total else 0.0
    val isSafe = current >= target
    val color = if (isSafe) StatusPresent else StatusAbsent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                        null,
                        tint = color
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                if (isSafe) {
                    val canMiss = PredictiveAttendanceUtils.calculateAffordableAbsences(attended, total, target.toFloat())
                    Text("Safe Zone", fontWeight = FontWeight.Black, color = color)
                    Text(
                        if (canMiss > 0) "You can miss $canMiss more classes while staying above 75%."
                        else "You are exactly on target. Don't miss the next class!",
                        fontSize = 13.sp
                    )
                } else {
                    val need = PredictiveAttendanceUtils.calculateRequiredClasses(attended, total, target.toFloat())
                    Text("Critical Standing", fontWeight = FontWeight.Black, color = color)
                    Text("Attend the next $need classes consecutively to reach 75%.", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun DetailBox(modifier: Modifier, label: String, value: String, color: Color) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.7f))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}
