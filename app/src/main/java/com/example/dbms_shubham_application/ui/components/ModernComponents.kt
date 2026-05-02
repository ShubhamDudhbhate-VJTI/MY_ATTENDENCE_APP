package com.example.dbms_shubham_application.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dbms_shubham_application.data.model.AttendanceRecord
import com.example.dbms_shubham_application.data.model.FacultySessionRecord
import com.example.dbms_shubham_application.data.model.SessionDetailsResponse
import com.example.dbms_shubham_application.utils.DateTimeUtils

@Composable
fun ModernAttendanceCard(
    record: AttendanceRecord,
    onClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val isPresent = (record.status ?: "absent").lowercase() == "present"
    val date = record.timestamp?.let { DateTimeUtils.formatDateOnly(it) } ?: "N/A"
    val time = record.timestamp?.let { DateTimeUtils.formatTimeOnly(it) } ?: "N/A"
    val statusColor = if (isPresent) Color(0xFF00C853) else Color(0xFFFF3D00)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = statusColor.copy(alpha = 0.2f))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.5.dp, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.linearGradient(listOf(statusColor.copy(alpha = 0.15f), statusColor.copy(alpha = 0.05f))),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.subject_name.ifEmpty { record.subject_id },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$date • $time", fontSize = 12.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            
            Surface(
                color = statusColor,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = record.status.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ModernReportCard(
    session: FacultySessionRecord,
    isDownloading: Boolean,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    onDownload: () -> Unit,
    onDownloadExcel: () -> Unit = {},
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val dateDisplay = DateTimeUtils.formatDateOnly(session.start_time)
    val timeDisplay = DateTimeUtils.formatTimeOnly(session.start_time)
    
    val status = session.status.lowercase()
    val (statusColor, statusIcon, cardGradient) = when {
        status.contains("stop") || status.contains("fail") || status.contains("cancel") -> 
            Triple(Color(0xFFFF5252), Icons.Default.Block, Brush.verticalGradient(listOf(Color(0xFFFF5252).copy(0.05f), Color.Transparent)))
        status.contains("active") -> 
            Triple(Color(0xFF2979FF), Icons.Default.Radar, Brush.verticalGradient(listOf(Color(0xFF2979FF).copy(0.05f), Color.Transparent)))
        else -> 
            Triple(Color(0xFF00E676), Icons.Default.CheckCircle, Brush.verticalGradient(listOf(Color(0xFF00E676).copy(0.05f), Color.Transparent)))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .shadow(
                elevation = if (isNew) 20.dp else 4.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = statusColor.copy(alpha = 0.5f)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(
            width = if (isNew) 1.5.dp else 0.5.dp,
            color = if (isNew) statusColor else colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Box(modifier = Modifier.background(cardGradient)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = session.subject_name.ifEmpty { "Subject: ${session.subject_id.take(8)}" },
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isNew) {
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.horizontalGradient(listOf(Color(0xFFFF5252), Color(0xFFFF8A65))),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("NEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = session.classroom_id.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                statusIcon, 
                                null, 
                                tint = statusColor, 
                                modifier = Modifier.size(12.dp).then(
                                    if (status.contains("active")) Modifier.graphicsLayer(scaleX = pulseScale, scaleY = pulseScale) else Modifier
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = session.status.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = statusColor
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateDisplay, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(Modifier.size(4.dp).background(colorScheme.outline.copy(0.3f), CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.AccessTime, null, tint = colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(timeDisplay, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary.copy(0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.People, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("${session.student_count}", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("Attendees", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant.copy(0.6f))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.padding(end = 4.dp).size(40.dp).background(colorScheme.error.copy(0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, tint = colorScheme.error, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = onDownloadExcel,
                            modifier = Modifier.padding(end = 4.dp).size(40.dp).background(colorScheme.secondary.copy(0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.TableChart, null, tint = colorScheme.secondary, modifier = Modifier.size(20.dp))
                        }

                        Button(
                            onClick = onDownload,
                            enabled = !isDownloading,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            modifier = Modifier.height(44.dp)
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("REPORT", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDetailsDialog(
    details: SessionDetailsResponse?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAddManual: (String) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        content = {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Session Analysis",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                "Detailed student attendance metrics",
                                fontSize = 12.sp,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd).background(colorScheme.surfaceVariant.copy(0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colorScheme.primary, strokeWidth = 5.dp)
                        }
                    } else if (details == null) {
                        Text("No analytical data found.", color = colorScheme.error)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Stats Overview
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatsBox(Modifier.weight(1f), "Participants", "${details.total_students}", Icons.Default.Groups)
                                StatsBox(Modifier.weight(1f), "Start Time", DateTimeUtils.formatTimeOnly(details.start_time), Icons.Default.HistoryToggleOff)
                            }
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ATTENDEE ROSTER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = colorScheme.primary, letterSpacing = 1.5.sp)
                                        IconButton(
                                            onClick = { onAddManual(details.session_id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.PersonAdd, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    LazyColumn(
                                        modifier = Modifier.heightIn(max = 350.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(details.students) { student ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(colorScheme.surface, RoundedCornerShape(14.dp))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colorScheme.primary.copy(0.1f),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(student.student_name.take(1).uppercase(), fontWeight = FontWeight.Black, color = colorScheme.primary)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(student.student_name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(student.student_id, fontSize = 11.sp, color = colorScheme.onSurfaceVariant.copy(0.6f))
                                                }
                                                Text(
                                                    DateTimeUtils.formatTimeOnly(student.marked_at),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(58.dp).shadow(8.dp, RoundedCornerShape(18.dp), spotColor = colorScheme.primary),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Finish Review", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
    )
}

@Composable
fun StatsBox(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color = colorScheme.primary.copy(0.05f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colorScheme.primary.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant.copy(0.7f))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = colorScheme.onSurface)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
