package com.example.dbms_shubham_application.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HODManageScreen(navController: NavController) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val manageItems = listOf(
        ManageAction("Faculty Members", "Assign and update faculty roles", Icons.Default.People, colorScheme.primary),
        ManageAction("Course Schedules", "Edit departmental class schedule", Icons.Default.CalendarToday, colorScheme.secondary),
        ManageAction("Academic Records", "Access departmental attendance logs", Icons.Default.Description, colorScheme.tertiary),
        ManageAction("Configuration", "Set department-wide parameters", Icons.Default.Settings, colorScheme.onSurface.copy(alpha = 0.6f))
    )

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        // Decorative Cyber Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to colorScheme.primary.copy(0.12f),
                    1.0f to Color.Transparent
                ),
                radius = 600.dp.toPx(),
                center = Offset(width * 0.9f, height * 0.05f)
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to colorScheme.secondary.copy(0.08f),
                    1.0f to Color.Transparent
                ),
                radius = 500.dp.toPx(),
                center = Offset(width * 0.1f, height * 0.45f)
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -20 }, animationSpec = tween(600))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(end = 48.dp)) {
                                Text(
                                    "Veermata Jijabai Technological Institute",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text("Department Management", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.5).sp)
                                Text("ADMINISTRATION HUB", color = colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(40.dp)
                                .background(colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                                .border(1.dp, colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colorScheme.onBackground, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
            ) {
                item {
                    Text(
                        "OPERATIONAL CONTROLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(manageItems) { action ->
                    ManageCard(action) {
                        when(action.title) {
                            "Faculty Members" -> navController.navigate("faculty_list")
                            "Course Schedules" -> navController.navigate("manage_schedule")
                            "Academic Records" -> navController.navigate("hod_analytics")
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(
                        "SYSTEM MONITOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.secondary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                       Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                           Box(
                               modifier = Modifier
                                   .size(12.dp)
                                   .background(Color(0xFF4CAF50), CircleShape)
                                   .shadow(4.dp, CircleShape, spotColor = Color(0xFF4CAF50))
                           )
                           Spacer(modifier = Modifier.width(16.dp))
                           Column {
                               Text("CENTRAL ATTENDANCE CORE", color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                               Text("Status: Synchronized & Operational", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                           }
                       }
                    }
                }
            }
        }
    }
}

data class ManageAction(val title: String, val description: String, val icon: ImageVector, val color: Color)

@Composable
fun ManageCard(action: ManageAction, onClick: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(action.color.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .border(1.dp, action.color.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(action.icon, null, tint = action.color, modifier = Modifier.size(24.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(action.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colorScheme.onSurface)
                Text(action.description, fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.6f))
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = colorScheme.primary.copy(alpha = 0.4f))
        }
    }
}

