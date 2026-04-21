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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
fun HODAnalyticsScreen(navController: NavController) {
    val colorScheme = MaterialTheme.colorScheme
    val stats = listOf(
        StatItem("Avg. Attendance", "82.5%", Icons.AutoMirrored.Filled.TrendingUp, colorScheme.primary),
        StatItem("Active Sessions", "14", Icons.Default.BarChart, colorScheme.secondary),
        StatItem("Low Attendance", "3", Icons.AutoMirrored.Filled.TrendingDown, colorScheme.error)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Department Analytics", fontWeight = FontWeight.Bold, color = colorScheme.onBackground) },
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    "Overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }

            // Stat Cards
            items(stats) { stat ->
                AnalyticsStatCard(stat)
            }

            item {
                Text(
                    "Department Performance",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            item {
                PerformanceChartPlaceholder()
            }
            
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

data class StatItem(val title: String, val value: String, val icon: ImageVector, val color: Color)

@Composable
fun AnalyticsStatCard(stat: StatItem) {
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
                    .size(50.dp)
                    .background(stat.color.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, stat.color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(stat.icon, null, tint = stat.color, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(stat.title, fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.6f))
                Text(stat.value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun PerformanceChartPlaceholder() {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BarChart, null, modifier = Modifier.size(60.dp), tint = colorScheme.tertiary.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Attendance Trend Visualizer", color = colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                Text("Data rendering coming soon", fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.4f))
            }
            
            // Decorative elements to look like a chart
            Canvas(modifier = Modifier.fillMaxSize()) {
                // We'd draw lines/bars here
            }
        }
    }
}

@Composable
fun Canvas(modifier: Modifier, onDraw: () -> Unit) {
    // Spacer for now as actual Canvas needs DrawScope
}
