package com.example.dbms_shubham_application.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
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
    
    // VJTI Professional Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Institutional Glow
        val glowColor = MaterialTheme.colorScheme.primary
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp).alpha(0.15f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.9f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Institutional Logo Container
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                        shape = RoundedCornerShape(40.dp)
                    )
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(38.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "VJTI",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            style = TextStyle(
                                brush = Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            ),
                            letterSpacing = (-1).sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // App Name with Professional Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textOffset.value.dp)
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = "Academic Portal",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "VEERMATA JIJABAI TECHNOLOGICAL INSTITUTE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Bottom Footer
        Text(
            text = "MATUNGA, MUMBAI • ESTD 1887",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(textAlpha.value * 0.7f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

private fun <T> overshootTween(duration: Int): TweenSpec<T> {
    return tween(
        durationMillis = duration,
        easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.4f) // Professional Overshoot
    )
}
