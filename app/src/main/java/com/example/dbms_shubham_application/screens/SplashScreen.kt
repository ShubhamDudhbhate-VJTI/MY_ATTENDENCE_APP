package com.example.dbms_shubham_application.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    // Animation States
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(20f) }
    
    LaunchedEffect(Unit) {
        // Start Logo Animation
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = overshootTween(800)
            )
        }
        launch {
            alpha.animateTo(1f, tween(1000))
        }
        
        // Staggered Text Animation
        delay(400)
        launch {
            textAlpha.animateTo(1f, tween(800))
        }
        launch {
            textOffset.animateTo(0f, overshootTween(800))
        }
        
        // Navigation Logic
        val isLoggedIn = sessionManager.isLoggedIn()
        val role = sessionManager.getRole() ?: "student"
        
        delay(2200) // Give enough time for the "tremendous" animation to finish
        
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
    
    // Deep Space Background with a subtle radial glow
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617)), // Match our new color.xml
        contentAlignment = Alignment.Center
    ) {
        // Ambient Glow in the background
        Canvas(modifier = Modifier.fillMaxSize().blur(80.dp).alpha(0.4f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2563EB), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.8f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // The Kinetic Logo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
                        shape = CircleShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color(0xFF020617) // Dark core
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "SD",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-2).sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // App Name with Staggered Overshoot
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textOffset.value.dp)
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = "Smart Detection",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                
                Surface(
                    color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        text = "SECURE INTELLIGENT SYSTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Bottom Branding
        Text(
            text = "POWERED BY BIOMETRIC AI",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(textAlpha.value * 0.5f),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 3.sp
        )
    }
}

private fun <T> overshootTween(duration: Int): TweenSpec<T> {
    return tween(
        durationMillis = duration,
        easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.4f) // Professional Overshoot
    )
}
