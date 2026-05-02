package com.example.dbms_shubham_application.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.components.ModernTextField
import kotlinx.coroutines.launch

enum class NotificationTarget {
    INDIVIDUAL, GROUP, CLASS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNotificationScreen(navController: NavController) {
    var selectedTarget by remember { mutableStateOf(NotificationTarget.CLASS) }
    var targetId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Broadcast", fontWeight = FontWeight.Black, color = colorScheme.onBackground, fontSize = 22.sp)
                        Text("Notify students & groups", color = colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                            .background(colorScheme.surface, CircleShape)
                            .border(1.dp, colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    "Recipient Scope",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colorScheme.onBackground
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TargetOption(
                        "Individual", 
                        Icons.Default.Person, 
                        selectedTarget == NotificationTarget.INDIVIDUAL,
                        Modifier.weight(1f)
                    ) { selectedTarget = NotificationTarget.INDIVIDUAL }
                    
                    TargetOption(
                        "Subject", 
                        Icons.AutoMirrored.Filled.MenuBook,
                        selectedTarget == NotificationTarget.GROUP,
                        Modifier.weight(1f)
                    ) { selectedTarget = NotificationTarget.GROUP }
                    
                    TargetOption(
                        "Class", 
                        Icons.Default.Groups, 
                        selectedTarget == NotificationTarget.CLASS,
                        Modifier.weight(1f)
                    ) { selectedTarget = NotificationTarget.CLASS }
                }
            }

            item {
                ModernTextField(
                    value = targetId,
                    onValueChange = { targetId = it },
                    label = when(selectedTarget) {
                        NotificationTarget.INDIVIDUAL -> "Registration ID"
                        NotificationTarget.GROUP -> "Branch|Year"
                        NotificationTarget.CLASS -> "Subject ID"
                    },
                    placeholder = when(selectedTarget) {
                        NotificationTarget.INDIVIDUAL -> "e.g. 241080017"
                        NotificationTarget.GROUP -> "e.g. Information Technology|Third Year"
                        NotificationTarget.CLASS -> "e.g. (Select from dropdown)"
                    },
                    icon = when(selectedTarget) {
                        NotificationTarget.INDIVIDUAL -> Icons.Default.Fingerprint
                        NotificationTarget.GROUP -> Icons.Default.Tag
                        NotificationTarget.CLASS -> Icons.Default.Hub
                    }
                )
            }

            item {
                Text(
                    "Announcement Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colorScheme.onBackground
                )
            }

            item {
                ModernTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Notification Title",
                    placeholder = "Enter a catchy headline",
                    icon = Icons.Default.Title
                )
            }

            item {
                ModernTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = "Detailed Message",
                    placeholder = "What would you like to say?",
                    icon = Icons.Default.ChatBubbleOutline,
                    singleLine = false,
                    modifier = Modifier.height(150.dp)
                )
            }

            item {
                Button(
                    onClick = {
                        if (title.isBlank() || message.isBlank() || targetId.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Please fill all fields") }
                            return@Button
                        }
                        
                        isLoading = true
                        scope.launch {
                            try {
                                val targetType = when(selectedTarget) {
                                    NotificationTarget.INDIVIDUAL -> "individual"
                                    NotificationTarget.GROUP -> "group"
                                    NotificationTarget.CLASS -> "class"
                                }
                                
                                val payload = mapOf(
                                    "target_type" to targetType,
                                    "target_id" to targetId,
                                    "title" to title,
                                    "message" to message,
                                    "sender_id" to (sessionManager.getUserId() ?: "")
                                )
                                
                                val response = RetrofitClient.apiService.sendNotification(payload)
                                if (response.isSuccessful) {
                                    snackbarHostState.showSnackbar("Broadcast sent successfully!")
                                    title = ""
                                    message = ""
                                    targetId = ""
                                } else {
                                    snackbarHostState.showSnackbar("Failed: ${response.errorBody()?.string()}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Send Broadcast", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TargetOption(
    label: String, 
    icon: ImageVector, 
    isSelected: Boolean, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) colorScheme.primary else colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isSelected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, 
                null, 
                tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label, 
                fontSize = 11.sp, 
                color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
            )
        }
    }
}
