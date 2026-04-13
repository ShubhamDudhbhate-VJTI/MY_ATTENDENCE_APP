package com.example.dbms_shubham_application.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.UserProfile
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val AccentRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val rawUserId = sessionManager.getUserId() ?: ""
    val userId = rawUserId.replace("\"", "").trim()
    
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(userId, retryTrigger) {
        if (userId.isNotEmpty()) {
            isLoading = true
            hasError = false
            try {
                val response = RetrofitClient.apiService.getUserProfile(userId)
                if (response.isSuccessful) {
                    userProfile = response.body()
                } else {
                    hasError = true
                }
            } catch (e: Exception) {
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (hasError) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentRed, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Could not load profile", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text("Check your internet or server connection", color = TextMuted, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { retryTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Try Again")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    item {
                        ProfileHeaderCard(userProfile)
                    }

                    item {
                        ProfileSectionTitle("Academic Information")
                        val academicItems = mutableListOf<Triple<ImageVector, String, String>>()
                        
                        userProfile?.let { profile ->
                            if (profile.role == "student") {
                                academicItems.add(Triple(Icons.Default.School, "Branch", profile.academic["branch"] ?: "Information Technology"))
                                academicItems.add(Triple(Icons.Default.Class, "Year", profile.academic["year"] ?: "S.Y. B.Tech"))
                                academicItems.add(Triple(Icons.Default.Fingerprint, "Roll Number", profile.academic["reg_no"] ?: userId))
                            } else {
                                academicItems.add(Triple(Icons.Default.School, "Department", profile.academic["branch"] ?: "Information Technology"))
                                academicItems.add(Triple(Icons.Default.Work, "Designation", profile.academic["designation"] ?: "Faculty"))
                                academicItems.add(Triple(Icons.Default.Badge, "Employee ID", profile.academic["employee_id"] ?: userId))
                            }
                        }

                        if (academicItems.isEmpty()) {
                            academicItems.add(Triple(Icons.Default.Info, "Status", "No academic records found"))
                        }

                        ProfileInfoCard(academicItems)
                    }

                    item {
                        ProfileSectionTitle("Settings")
                        SettingsCard(navController, sessionManager)
                    }

                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Version 1.0.0 (Stable)",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderCard(profile: UserProfile?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AccentBlue, Color(0xFF8B5CF6)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (profile?.full_name?.take(1) ?: profile?.username?.take(1) ?: "U").uppercase(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = profile?.full_name ?: "User Name",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            
            Text(
                text = profile?.email ?: "vjti.student@vjti.ac.in",
                fontSize = 14.sp,
                color = TextMuted
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileMiniStat("Role", profile?.role?.replaceFirstChar { it.uppercase() } ?: "User")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(TextMuted.copy(alpha = 0.2f)))
                ProfileMiniStat("ID", profile?.username ?: "N/A")
            }
        }
    }
}

@Composable
fun ProfileMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Text(label, fontSize = 12.sp, color = TextMuted)
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextWhite,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ProfileInfoCard(items: List<Triple<ImageVector, String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AccentBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.first, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(item.second, fontSize = 12.sp, color = TextMuted)
                        Text(item.third, fontSize = 14.sp, color = TextWhite, fontWeight = FontWeight.Medium)
                    }
                }
                if (index < items.size - 1) {
                    HorizontalDivider(color = TextWhite.copy(alpha = 0.05f), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun SettingsCard(navController: NavController, sessionManager: SessionManager) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            SettingsRow(Icons.Default.Notifications, "Notifications", "App alerts and reminders")
            SettingsRow(Icons.Default.Security, "Privacy & Security", "Manage biometric data")
            SettingsRow(Icons.Default.Help, "Help Center", "FAQs and support")
            
            HorizontalDivider(color = TextWhite.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        // 1. Clear the local user session
                        sessionManager.logout()
                        
                        // 2. Navigate back to role selection and clear the entire backstack
                        navController.navigate("role_selection") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AccentRed, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Sign Out", color = AccentRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextWhite, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}
