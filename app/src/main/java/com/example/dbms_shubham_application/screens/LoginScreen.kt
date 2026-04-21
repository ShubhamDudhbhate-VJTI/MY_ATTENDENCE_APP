package com.example.dbms_shubham_application.screens

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
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (-120).dp, y = (-120).dp)
                .background(primaryColor.copy(alpha = 0.12f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Icon with Gradient
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(primaryColor, secondaryColor)))
                    .border(1.dp, onBackground.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(role.lowercase()) {
                        "student" -> Icons.Default.Person
                        "faculty" -> Icons.Default.School
                        else -> Icons.Default.AdminPanelSettings
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "${role.replaceFirstChar { it.uppercase() }} Portal",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = onBackground,
                letterSpacing = (-1).sp
            )
            
            Text(
                text = "Secure access to your dashboard",
                fontSize = 15.sp,
                color = onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )
            
            // Input Fields
            ModernTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username / ID",
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
            
            Spacer(modifier = Modifier.height(48.dp))
            
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
                                val errorMsg = when (response.code()) {
                                    503, 504 -> "Server is starting up, please wait a moment."
                                    404 -> "User not found. Please check your credentials."
                                    401 -> "Invalid password. Try again."
                                    else -> "Authentication Failed (${response.code()}): ${response.message()}"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            val errorMsg = when (e) {
                                is java.net.SocketTimeoutException -> "Connection Timeout: Check your Wi-Fi signal."
                                is java.net.ConnectException -> "Cannot reach Server: Ensure PC and Mobile are on same Wi-Fi."
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
                    containerColor = primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
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
