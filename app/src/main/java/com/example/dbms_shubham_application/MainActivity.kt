package com.example.dbms_shubham_application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.dbms_shubham_application.navigation.AppNavigation
import com.example.dbms_shubham_application.ui.theme.DBMS_Shubham_ApplicationTheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import android.os.Build
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.service.MyFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : ComponentActivity() {
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MyFirebaseMessagingService.CHANNEL_ID,
                MyFirebaseMessagingService.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts and attendance notifications"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Firebase
        com.google.firebase.FirebaseApp.initializeApp(this)
        
        createNotificationChannel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val sessionManager = SessionManager(this)
        
        // Sync FCM Token for notifications
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userId = sessionManager.getUserId()
                if (userId != null && token != null) {
                    MainScope().launch {
                        try {
                            RetrofitClient.apiService.updateFcmToken(mapOf(
                                "user_id" to userId,
                                "fcm_token" to token
                            ))
                            Log.d("FCM", "Token synced successfully: $token")
                        } catch (e: Exception) {
                            Log.e("FCM", "Token sync failed", e)
                        }
                    }
                }
            }
        }
        
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDark by remember { mutableStateOf(sessionManager.isDarkMode(systemDark)) }
            
            DBMS_Shubham_ApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        isDark = isDark,
                        onThemeChange = { newMode -> 
                            isDark = newMode
                            sessionManager.setDarkMode(newMode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashContent(modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
            text = "Smart Attendance",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onPrimary,
            modifier = Modifier.padding(top = 24.dp)
        )

        Text(
            text = "Face Recognition & QR Code Based",
            fontSize = 16.sp,
            color = colorScheme.onPrimary.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureCard(
                title = "Face Recognition",
                description = "AI-powered attendance marking",
                icon = "face"
            )
            FeatureCard(
                title = "QR Code Scanner",
                description = "Quick check-in with QR codes",
                icon = "qr_code"
            )
            FeatureCard(
                title = "WiFi Detection",
                description = "Location-based verification",
                icon = "wifi"
            )
            FeatureCard(
                title = "Real-time Reports",
                description = "Track attendance instantly",
                icon = "analytics"
            )
        }

        Button(
            onClick = { /* TODO: Navigate to Login */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 32.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.onPrimary,
                contentColor = colorScheme.primary
            )
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        }
    }
}

@Composable
fun FeatureCard(title: String, description: String, icon: String) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        colorScheme.primary.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon.first().uppercaseChar().toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DBMS_Shubham_ApplicationTheme {
        SplashContent()
    }
}