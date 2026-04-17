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
import kotlinx.coroutines.launch

// --- THEME CONSISTENCY ---
private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val GlassBorder = Color(0xFF334155)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentPurple = Color(0xFF8B5CF6)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)

@Composable
fun LoginScreen(navController: NavController, role: String) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .systemBarsPadding()
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (-120).dp, y = (-120).dp)
                .background(AccentBlue.copy(alpha = 0.12f), CircleShape)
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
                    .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple)))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
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
                    modifier = Modifier.size(44.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "${role.replaceFirstChar { it.uppercase() }} Portal",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                letterSpacing = (-1).sp
            )
            
            Text(
                text = "Secure access to your dashboard",
                fontSize = 15.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )
            
            // Input Fields
            ModernTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username / ID",
                icon = Icons.Default.AlternateEmail,
                keyboardType = KeyboardType.Text
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
                onPasswordToggle = { passwordVisible = !passwordVisible }
            )
            
            Text(
                text = "Forgot Password?",
                color = AccentBlue,
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
                                val errorMsg = if (response.code() == 503 || response.code() == 504) {
                                    "Server is starting up, please wait a moment and try again."
                                } else {
                                    "Authentication Failed: ${response.message()}"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            val errorMsg = if (e is java.net.SocketTimeoutException || e is java.io.IOException) {
                                "Server is starting up, please wait a moment and try again."
                            } else {
                                "Network Error: ${e.message}"
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(20.dp),
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                } else {
                    Text("Secure Login", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Not registered yet?", color = TextMuted, fontSize = 14.sp)
                Text(
                    text = " Create Account",
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { navController.navigate("signup/$role") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = GlassBorder,
            cursorColor = AccentBlue,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedContainerColor = CardBg.copy(alpha = 0.5f),
            unfocusedContainerColor = CardBg.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp),
        singleLine = true
    )
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
