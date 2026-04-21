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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.NotificationRecord
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.Duration

// --- THEME CONSISTENCY REMOVED LEGACY COLORS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId = sessionManager.getUserId() ?: ""
    val scope = rememberCoroutineScope()
    
    var notifications by remember { mutableStateOf<List<NotificationRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isClearing by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme

    fun fetchNotifications() {
        if (userId.isEmpty()) return
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getNotifications(userId)
                if (response.isSuccessful) {
                    notifications = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("AlertsScreen", "Error fetching notifications", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun clearAllNotifications() {
        if (userId.isEmpty()) return
        isClearing = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.clearAllNotifications(userId)
                if (response.isSuccessful) {
                    notifications = emptyList()
                }
            } catch (e: Exception) {
                Log.e("AlertsScreen", "Error clearing notifications", e)
            } finally {
                isClearing = false
            }
        }
    }

    LaunchedEffect(userId) {
        fetchNotifications()
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Alerts & Notifications", color = colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Important updates and reminders", color = colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Normal)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorScheme.onBackground)
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { clearAllNotifications() }, enabled = !isClearing) {
                            if (isClearing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = colorScheme.error)
                            }
                        }
                    }
                    IconButton(onClick = { fetchNotifications() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No notifications yet", color = colorScheme.onBackground.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                items(notifications) { alert ->
                    AlertCard(alert) {
                        // Optional: Mark as read when clicked
                        scope.launch {
                            RetrofitClient.apiService.markNotificationAsRead(alert.id)
                            fetchNotifications() // Refresh
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: NotificationRecord, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    
    val type = when {
        alert.title.contains("Warning", ignoreCase = true) -> "High"
        alert.title.contains("Started", ignoreCase = true) -> "Medium"
        else -> "Info"
    }

    val icon = when (type) {
        "High" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }
    
    // We can use theme colors or specific accent colors
    val iconColor = when (type) {
        "High" -> MaterialTheme.colorScheme.error
        "Medium" -> colorScheme.secondary
        else -> colorScheme.primary
    }

    val timeAgo = try {
        val createdAt = ZonedDateTime.parse(alert.created_at)
        val now = ZonedDateTime.now()
        val duration = Duration.between(createdAt, now)
        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
            duration.toHours() < 24 -> "${duration.toHours()}h ago"
            else -> "${duration.toDays()}d ago"
        }
    } catch (e: Exception) {
        alert.created_at.take(10)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (alert.is_read) Color.Transparent else colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.is_read) colorScheme.surface.copy(alpha = 0.7f) else colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(alert.title, color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(timeAgo, color = colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = alert.message,
                    color = colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                
                if (!alert.is_read) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "New Update • Mark as read",
                        color = iconColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
