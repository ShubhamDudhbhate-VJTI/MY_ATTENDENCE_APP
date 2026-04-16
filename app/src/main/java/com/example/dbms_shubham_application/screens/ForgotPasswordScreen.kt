package com.example.dbms_shubham_application.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private val DarkBg = Color(0xFF0F172A)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentPurple = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Reset Password", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple)))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LockReset, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Trouble logging in?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Enter your institutional email and we'll send you a link to reset your password.",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            ModernTextField(
                value = email,
                onValueChange = { email = it },
                label = "Institutional Email",
                icon = Icons.Default.Email
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = { /* Handle Reset */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Send Reset Link", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(onClick = { navController.navigateUp() }) {
                Text("Back to Sign In", color = TextMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}
