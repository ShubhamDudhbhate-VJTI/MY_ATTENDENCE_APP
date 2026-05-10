package com.example.dbms_shubham_application.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.components.ModernTextField
import kotlinx.coroutines.launch

// --- THEME CONSISTENCY REMOVED LEGACY COLORS ---

@Composable
fun LoginScreen(navController: NavController, role: String) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    // Get colors from theme
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onBackground = MaterialTheme.colorScheme.onBackground
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding()
    ) {
        // Decorative background elements
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .offset(x = (-120).dp, y = (-120).dp)
                    .background(primaryColor.copy(alpha = 0.12f), CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -40 }, animationSpec = tween(600))
            ) {
                // Enterprise-grade Logo Container
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(primaryColor, secondaryColor)
                            )
                        )
                        .padding(2.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(backgroundColor)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(primaryColor, secondaryColor))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when(role.lowercase()) {
                                "student" -> Icons.Default.Person
                                "faculty" -> Icons.Default.School
                                else -> Icons.Default.AdminPanelSettings
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 100)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, 100))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${role.replaceFirstChar { it.uppercase() }} Access",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = onBackground
                    )
                    
                    Text(
                        text = "Sign in to VJTI Academic Portal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 40.dp)
                    )
                }
            }
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 200)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(600, 200))
            ) {
                Column {
                    // Input Fields
                    ModernTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username / ID",
                        placeholder = "e.g. 2021001 or name@college.edu",
                        icon = Icons.Default.AlternateEmail,
                        keyboardType = KeyboardType.Text,
                        colors = primaryColor to outlineColor,
                        textColor = onBackground,
                        surfaceColor = surfaceColor
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    ModernTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.LockOpen,
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        colors = primaryColor to outlineColor,
                        textColor = onBackground,
                        surfaceColor = surfaceColor
                    )
                    
                    Text(
                        text = "Forgot Password?",
                        color = primaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 16.dp)
                            .clickable { navController.navigate("forgot_password") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 300)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(600, 300))
            ) {
                // Sign In Button
                Button(
                    onClick = {
                        isLoading = true
                         scope.launch {
                            try {
                                val credentials = mapOf(
                                    "username" to username.trim(),
                                    "password" to password.trim()
                                )
                                val response = RetrofitClient.apiService.login(credentials)
                                if (response.isSuccessful && response.body() != null) {
                                    val body = response.body()!!
                                    val sessionManager = SessionManager(context)
                                    
                                    val userId = body.user_id ?: ""
                                    val userRole = body.role ?: role.lowercase()
                                    val userName = body.name ?: ""
                                    
                                    sessionManager.saveSession(userId, userRole, userName)

                                    // --- SYNC FCM TOKEN ON LOGIN ---
                                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val token = task.result
                                            scope.launch {
                                                try {
                                                    RetrofitClient.apiService.updateFcmToken(mapOf(
                                                        "user_id" to userId,
                                                        "fcm_token" to token
                                                    ))
                                                    android.util.Log.d("FCM", "Token synced on login: $token")
                                                } catch (e: Exception) {
                                                    android.util.Log.e("FCM", "Token sync failed on login", e)
                                                }
                                            }
                                        }
                                    }

                                    if (userRole == "student") {
                                        scope.launch {
                                            try {
                                                val profileRes = RetrofitClient.apiService.getUserProfile(userId)
                                                if (profileRes.isSuccessful) {
                                                    val imageUrl = profileRes.body()?.image_url
                                                    if (!imageUrl.isNullOrBlank()) {
                                                        downloadAndSaveFace(context, userId, imageUrl)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("Login", "Failed to cache master face: ${e.message}")
                                            }
                                        }
                                    }

                                    navController.navigate("dashboard/$userRole") {
                                        popUpTo("role_selection") { inclusive = false }
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    val errorMsg = when (response.code()) {
                                        503, 504 -> "Server is starting up (Render Cold Start). Please wait 30 seconds and try again."
                                        404 -> "Server Error (404): Endpoint not found. Check if the Render URL is correct or if the user exists in the cloud database."
                                        401 -> "Invalid credentials. Please check your password."
                                        else -> "Authentication Failed (${response.code()}): ${errorBody ?: response.message()}"
                                    }
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                val errorMsg = when (e) {
                                    is java.net.SocketTimeoutException -> "Connection Timeout: Check your USB connection."
                                    is java.net.ConnectException -> "Cannot reach Server: Run 'adb reverse tcp:8000 tcp:8000'."
                                    is java.io.IOException -> "Network Error: ${e.localizedMessage}"
                                    else -> "Error: ${e.message}"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(role.lowercase() == "student") secondaryColor else primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    } else {
                        Text("Secure Login", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 400))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Not registered yet?", color = onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
                    Text(
                        text = " Create Account",
                        color = primaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clickable { navController.navigate("signup/$role") }
                    )
                }
            }
        }
    }
}

private suspend fun downloadAndSaveFace(context: android.content.Context, userId: String, url: String) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bytes = response.body?.bytes() ?: return@withContext
                val faceDir = java.io.File(context.cacheDir, "face")
                if (!faceDir.exists()) faceDir.mkdirs()

                val masterFile = java.io.File(faceDir, "master_face_${userId}.jpg")
                masterFile.writeBytes(bytes)
                android.util.Log.d("Login", "Master face cached: ${masterFile.absolutePath}")
            }
        } catch (e: Exception) {
            android.util.Log.e("Login", "Error caching face: ${e.message}")
        }
    }
}
