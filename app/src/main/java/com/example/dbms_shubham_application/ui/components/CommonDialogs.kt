package com.example.dbms_shubham_application.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
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
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (initialRecord == null) "New Schedule" else "Edit Schedule",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.onSurface
                    )

                    // Subject Selector
                    ModernDialogDropdown(
                        label = "Subject",
                        selected = selectedSubject?.name ?: "Select Subject",
                        options = subjects.map { it.name },
                        icon = Icons.AutoMirrored.Filled.Subject,
                        onSelect = { name -> selectedSubject = subjects.find { it.name == name } }
                    )

                    // Day Selector
                    ModernDialogDropdown(
                        label = "Day of Week",
                        selected = selectedDay,
                        options = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"),
                        icon = Icons.Default.CalendarToday,
                        onSelect = { selectedDay = it }
                    )

                    // Room Selector
                    ModernDialogDropdown(
                        label = "Classroom",
                        selected = selectedRoom?.name ?: "Select Room",
                        options = classrooms.map { it.name },
                        icon = Icons.Default.MeetingRoom,
                        onSelect = { name -> selectedRoom = classrooms.find { it.name == name } }
                    )

                    ModernTextField(
                        value = timeStr,
                        onValueChange = { timeStr = it },
                        label = "Time Slot",
                        placeholder = "e.g. 09:00 AM - 10:00 AM",
                        icon = Icons.Default.AccessTime,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Schedule", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ModernDialogDropdown(
    label: String,
    selected: String,
    options: List<String>,
    icon: ImageVector,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(12.dp),
                border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(selected, fontSize = 14.sp)
                    }
                    Icon(Icons.Default.ArrowDropDown, null, tint = colorScheme.outline)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .background(colorScheme.surface, RoundedCornerShape(12.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
