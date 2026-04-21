package com.example.dbms_shubham_application.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dbms_shubham_application.data.model.Classroom
import com.example.dbms_shubham_application.data.model.ScheduleRecord
import com.example.dbms_shubham_application.data.model.Subject

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

    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        title = { Text(if (initialRecord == null) "Add Schedule" else "Edit Schedule", color = colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                // Day Selector
                val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                var dayExpanded by remember { mutableStateOf(false) }
                Column {
                    Text("Day", style = MaterialTheme.typography.labelMedium, color = colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { dayExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedDay, color = colorScheme.onSurface)
                                Icon(Icons.Default.ArrowDropDown, null, tint = colorScheme.primary)
                            }
                        }
                        DropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false },
                            modifier = Modifier.background(colorScheme.surface)
                        ) {
                            days.forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(day, color = colorScheme.onSurface) },
                                    onClick = { selectedDay = day; dayExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Subject Selector
                var subExpanded by remember { mutableStateOf(false) }
                Column {
                    Text("Subject", style = MaterialTheme.typography.labelMedium, color = colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { subExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedSubject?.name ?: "Select Subject", color = colorScheme.onSurface)
                                Icon(Icons.Default.ArrowDropDown, null, tint = colorScheme.primary)
                            }
                        }
                        DropdownMenu(
                            expanded = subExpanded,
                            onDismissRequest = { subExpanded = false },
                            modifier = Modifier.background(colorScheme.surface)
                        ) {
                            subjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name, color = colorScheme.onSurface) },
                                    onClick = { selectedSubject = sub; subExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Room Selector
                var roomExpanded by remember { mutableStateOf(false) }
                Column {
                    Text("Classroom", style = MaterialTheme.typography.labelMedium, color = colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { roomExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedRoom?.name ?: "Select Room", color = colorScheme.onSurface)
                                Icon(Icons.Default.ArrowDropDown, null, tint = colorScheme.primary)
                            }
                        }
                        DropdownMenu(
                            expanded = roomExpanded,
                            onDismissRequest = { roomExpanded = false },
                            modifier = Modifier.background(colorScheme.surface)
                        ) {
                            classrooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text(room.name, color = colorScheme.onSurface) },
                                    onClick = { selectedRoom = room; roomExpanded = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = timeStr,
                    onValueChange = { timeStr = it },
                    label = { Text("Time (e.g. 10:00 AM - 11:00 AM)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
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
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) { Text("Save Schedule", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colorScheme.primary)
            }
        }
    )
}
