package com.example.dbms_shubham_application.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
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
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val GlassBorder = Color(0xFF334155)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val SuccessGreen = Color(0xFF10B981)

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Send Notification", fontWeight = FontWeight.Bold, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
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
                    "Select Recipients",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TargetOption(
                        "Individual", 
                        Icons.Default.Person, 
                        selectedTarget == NotificationTarget.INDIVIDUAL,
                        Modifier.weight(1f)
                    ) { selectedTarget = NotificationTarget.INDIVIDUAL }
                    
                    TargetOption(
                        "Group", 
                        Icons.Default.Group, 
                        selectedTarget == NotificationTarget.GROUP,
                        Modifier.weight(1f)
                    ) { selectedTarget = NotificationTarget.GROUP }
                    
                    TargetOption(
                        "Whole Class", 
                        Icons.Default.School, 
                        selectedTarget == NotificationTarget.CLASS,
                        Modifier.weight(1f)
                    ) { selectedTarget = NotificationTarget.CLASS }
                }
            }

            item {
                OutlinedTextField(
                    value = targetId,
                    onValueChange = { targetId = it },
                    label = { 
                        Text(when(selectedTarget) {
                            NotificationTarget.INDIVIDUAL -> "Registration Number (e.g. 241080017)"
                            NotificationTarget.GROUP -> "Subject Name (e.g. DBMS)"
                            NotificationTarget.CLASS -> "Branch-Year (e.g. Information Technology-Second Year)"
                        })
                    },
                    placeholder = {
                        Text(when(selectedTarget) {
                            NotificationTarget.INDIVIDUAL -> "241080017"
                            NotificationTarget.GROUP -> "DBMS"
                            NotificationTarget.CLASS -> "Information Technology-Second Year"
                        }, color = TextMuted.copy(alpha = 0.5f))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Text(
                    "Message Content",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp)
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
                                    snackbarHostState.showSnackbar("Notification sent successfully!")
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
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Now", fontWeight = FontWeight.Bold)
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
    Column(
        modifier = modifier
            .background(
                if (isSelected) AccentBlue.copy(alpha = 0.1f) else CardBg,
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isSelected) AccentBlue else GlassBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon, 
            null, 
            tint = if (isSelected) AccentBlue else TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label, 
            fontSize = 12.sp, 
            color = if (isSelected) AccentBlue else TextMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
