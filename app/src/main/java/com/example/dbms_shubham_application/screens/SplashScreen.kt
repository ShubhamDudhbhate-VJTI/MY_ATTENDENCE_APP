package com.example.dbms_shubham_application.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    // Use a single side effect for navigation
    LaunchedEffect(Unit) {
        val isLoggedIn = sessionManager.isLoggedIn()
        val role = sessionManager.getRole() ?: "student"
        
        delay(1500) // Slightly shorter delay for better UX
        
        if (isLoggedIn) {
            navController.navigate("dashboard/$role") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("role_selection") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Match theme background to avoid flash
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with optimized background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SA",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2196F3)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Smart Attendance",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            
            Text(
                text = "Face Recognition & QR Powered",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            CircularProgressIndicator(
                color = Color(0xFF2196F3),
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
