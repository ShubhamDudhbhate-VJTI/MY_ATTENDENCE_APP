package com.example.dbms_shubham_application.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val AccentRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Profile Header
            item {
                ProfileHeaderCard()
            }

            // Academic Info
            item {
                ProfileSectionTitle("Academic Information")
                ProfileInfoCard(
                    listOf(
                        Triple(Icons.Default.School, "Department", "Information Technology"),
                        Triple(Icons.Default.Class, "Year", "2nd Year (S.Y. B.Tech)"),
                        Triple(Icons.Default.Fingerprint, "Roll Number", "241080017")
                    )
                )
            }

            // Settings
            item {
                ProfileSectionTitle("Settings")
                SettingsCard(navController)
            }

            // Version
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

@Composable
fun ProfileHeaderCard() {
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
                    text = "S",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Shubham",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            
            Text(
                text = "shubham.it@university.edu",
                fontSize = 14.sp,
                color = TextMuted
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileMiniStat("Attendance", "83.2%")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(TextMuted.copy(alpha = 0.2f)))
                ProfileMiniStat("Credits", "42.0")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(TextMuted.copy(alpha = 0.2f)))
                ProfileMiniStat("GPA", "9.1")
            }
        }
    }
}

@Composable
fun ProfileMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
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
fun SettingsCard(navController: NavController) {
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
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
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
