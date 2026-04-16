package com.example.dbms_shubham_application.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
            .background(Color(0xFF0F172A)), 
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))),
                        shape = CircleShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AX",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "AttendX",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Next-Gen Attendance System",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            CircularProgressIndicator(
                color = Color(0xFF3B82F6),
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
