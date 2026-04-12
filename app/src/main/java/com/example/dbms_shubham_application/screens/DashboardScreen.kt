package com.example.dbms_shubham_application.screens

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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentOrange = Color(0xFFF59E0B)
private val AccentRed = Color(0xFFEF4444)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)

@Composable
fun DashboardScreen(navController: NavController, role: String) {
    val roleTitle = when (role.lowercase()) {
        "student" -> "Student Portal"
        "faculty" -> "Faculty Dashboard"
        "hod" -> "HOD Administration"
        else -> "Dashboard"
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController, role) },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                item {
                    HeaderSection(navController, roleTitle)
                }

                // Greeting
                item {
                    GreetingSection(role)
                }

                // Stats Row
                item {
                    StatsRow(role)
                }

                // Quick Actions (New Section)
                if (role.lowercase() == "student") {
                    item {
                        QuickActionsSection(navController)
                    }
                } else if (role.lowercase() == "faculty") {
                    item {
                        FacultyQuickActionsSection(navController)
                    }
                }

                // Two main cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RecentAttendanceCard(modifier = Modifier.weight(1f), role = role)
                        TodayScheduleCard(modifier = Modifier.weight(1f), role = role)
                    }
                }
            }

            // Side Mark Attendance Button (Only for students)
            if (role.lowercase() == "student") {
                MarkAttendanceSideButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 20.dp),
                    onClick = { navController.navigate("mark_attendance") }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(navController: NavController, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
        }
        
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AccentBlue, Color(0xFF8B5CF6))))
                .clickable { navController.navigate("profile") },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = TextWhite)
        }
    }
}

@Composable
fun GreetingSection(role: String) {
    val name = if (role.lowercase() == "student") "Shubham" else "Prof. Deshmukh"
    val subtext = when (role.lowercase()) {
        "student" -> "S.Y. B.Tech (I.T.) Batch A • 241080017"
        "faculty" -> "Information Technology Department"
        "hod" -> "Head of IT Department"
        else -> ""
    }
    Column {
        Text(
            text = "Welcome back, $name",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = subtext,
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun StatsRow(role: String) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (role.lowercase() == "student") {
            item { StatCard("Overall Attendance", "83.2%", "Eligible", AccentBlue) }
            item { StatCard("Sessions Attended", "124", "of 149 total", Color(0xFF10B981)) }
            item { StatCard("Sessions Missed", "25", "3 this week", AccentOrange) }
            item { StatCard("At-Risk Subjects", "2", "Below 75%", AccentRed) }
        } else {
            item { StatCard("Active Classes", "4", "Today", AccentBlue) }
            item { StatCard("Avg. Attendance", "88%", "Across subjects", Color(0xFF10B981)) }
            item { StatCard("Pending Alerts", "12", "Flagged students", AccentOrange) }
            item { StatCard("Upcoming Exams", "1", "Internal Assessment", AccentRed) }
        }
    }
}

@Composable
fun QuickActionsSection(navController: NavController) {
    Column {
        Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard("History", Icons.Default.History, AccentBlue, modifier = Modifier.weight(1f)) {
                navController.navigate("attendance_history")
            }
            QuickActionCard("Leave", Icons.Default.EventBusy, AccentOrange, modifier = Modifier.weight(1f)) {
                /* TODO */
            }
            QuickActionCard("Support", Icons.Default.SupportAgent, Color(0xFF8B5CF6), modifier = Modifier.weight(1f)) {
                /* TODO */
            }
        }
    }
}

@Composable
fun FacultyQuickActionsSection(navController: NavController) {
    Column {
        Text("Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard("Start Session", Icons.Default.QrCode, AccentBlue, modifier = Modifier.weight(1f)) {
                navController.navigate("start_session")
            }
            QuickActionCard("Reports", Icons.Default.BarChart, AccentOrange, modifier = Modifier.weight(1f)) {
                navController.navigate("reports")
            }
            QuickActionCard("Classes", Icons.AutoMirrored.Filled.Assignment, Color(0xFF8B5CF6), modifier = Modifier.weight(1f)) {
                navController.navigate("faculty_classes")
            }
        }
    }
}

@Composable
fun QuickActionCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun StatCard(title: String, value: String, subtitle: String, color: Color) {
    Card(
        modifier = Modifier.width(110.dp).height(150.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 12.sp, color = TextMuted, lineHeight = 14.sp)
            Column {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun RecentAttendanceCard(modifier: Modifier = Modifier, role: String) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = if (role.lowercase() == "student") "Recent Attendance" else "Class Reports"
            val subtitle = if (role.lowercase() == "student") "Last 6 records" else "Recent sessions"
            
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("04 Feb", fontSize = 12.sp, color = TextMuted)
                    Text("Database Mgmt", fontSize = 14.sp, color = TextWhite)
                }
                val statusColor = if (role.lowercase() == "student") AccentRed else Color(0xFF10B981)
                val statusText = if (role.lowercase() == "student") "Absent" else "92%"
                
                Box(
                    modifier = Modifier.background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusText, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TodayScheduleCard(modifier: Modifier = Modifier, role: String) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = if (role.lowercase() == "student") "Today's Schedule" else "Teaching Load"
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
            Text("09:00 AM", fontSize = 12.sp, color = TextMuted)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (role.lowercase() == "student") "Database Mgmt Sys" else "SE-IT Batch B", fontSize = 14.sp, color = TextWhite, modifier = Modifier.weight(1f))
                Text("R-301", fontSize = 12.sp, color = AccentBlue)
            }
        }
    }
}

@Composable
fun MarkAttendanceSideButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .width(60.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp))
            .background(AccentBlue)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            "Mark Attendance".forEach { char ->
                Text(
                    text = char.toString().uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, role: String) {
    NavigationBar(
        containerColor = Color(0xFF151F32),
        tonalElevation = 8.dp
    ) {
        val items = if (role.lowercase() == "student") {
            listOf(
                Triple("Dashboard", Icons.Default.Dashboard, "dashboard/student"),
                Triple("Attendance", Icons.AutoMirrored.Filled.Assignment, "attendance_history"),
                Triple("Mark", Icons.Default.QrCodeScanner, "mark_attendance"),
                Triple("Alerts", Icons.Default.Notifications, "alerts")
            )
        } else {
            listOf(
                Triple("Dashboard", Icons.Default.Dashboard, "dashboard/$role"),
                Triple("My Classes", Icons.AutoMirrored.Filled.Assignment, "faculty_classes"),
                Triple("Reports", Icons.Default.BarChart, "reports"),
                Triple("Alerts", Icons.Default.Notifications, "alerts")
            )
        }

        items.forEach { (label, icon, route) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                selected = route == "dashboard/$role",
                onClick = { 
                    if (route.isNotEmpty()) {
                        navController.navigate(route)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    selectedIconColor = AccentBlue,
                    indicatorColor = AccentBlue.copy(alpha = 0.1f)
                )
            )
        }
    }
}


data class AttendanceStats(
    val totalClasses: Int,
    val presentClasses: Int,
    val absentClasses: Int,
    val percentage: Double
)


data class ScheduleItem(
    val subject: String,
    val time: String,
    val status: String
)
