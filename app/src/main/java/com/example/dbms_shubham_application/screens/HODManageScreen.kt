package com.example.dbms_shubham_application.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    
    val manageItems = listOf(
        ManageAction("Faculty Members", "Assign and update faculty roles", Icons.Default.People, colorScheme.primary),
        ManageAction("Course Schedules", "Edit departmental class schedule", Icons.Default.CalendarToday, colorScheme.secondary),
        ManageAction("Academic Records", "Access departmental attendance logs", Icons.Default.Description, colorScheme.tertiary),
        ManageAction("Configuration", "Set department-wide parameters", Icons.Default.Settings, colorScheme.onSurface.copy(alpha = 0.6f))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Department Management", fontWeight = FontWeight.Bold, color = colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background)
            )
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Department Operations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            items(manageItems) { action ->
                ManageCard(action)
            }
            
            item {
                Spacer(modifier = Modifier.height(30.dp))
                // Quick info or recent activity
                Text("System Health", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
                ) {
                   Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                       Box(modifier = Modifier.size(10.dp).background(colorScheme.primary, CircleShape))
                       Spacer(modifier = Modifier.width(12.dp))
                       Text("Attendance System Online", color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                   }
                }
            }
        }
    }
}

data class ManageAction(val title: String, val description: String, val icon: ImageVector, val color: Color)

@Composable
fun ManageCard(action: ManageAction) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(action.color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(action.icon, null, tint = action.color, modifier = Modifier.size(22.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(action.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Text(action.description, fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.6f))
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}
