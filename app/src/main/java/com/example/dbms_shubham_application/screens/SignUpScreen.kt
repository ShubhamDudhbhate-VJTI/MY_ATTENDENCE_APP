package com.example.dbms_shubham_application.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentPurple = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController, role: String) {
    var userIdInput by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 200.dp, y = (-100).dp)
                .background(AccentPurple.copy(alpha = 0.1f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Rebranded Logo/Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple)))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Join AttendX",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                letterSpacing = (-1).sp
            )
            
            Text(
                text = "Create your ${role} account to get started",
                fontSize = 15.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            ModernTextField(
                value = userIdInput,
                onValueChange = { userIdInput = it },
                label = if (role.lowercase() == "student") "Registration Number" else "Employee ID",
                icon = Icons.Default.Badge
            )

            Spacer(modifier = Modifier.height(20.dp))

            ModernTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(20.dp))

            ModernTextField(
                value = email,
                onValueChange = { email = it },
                label = "Institutional Email",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(20.dp))

            ModernTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                icon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (userIdInput.isBlank() || name.isBlank() || email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val userData = mapOf(
                                "id" to userIdInput.trim(),
                                "full_name" to name.trim(),
                                "email" to email.trim(),
                                "password" to password.trim(),
                                "role" to role.lowercase()
                            )
                            val response = RetrofitClient.apiService.signup(userData)
                            if (response.isSuccessful && response.body()?.get("success") == true) {
                                val body = response.body()!!
                                val finalUserId = body["user_id"]?.toString() ?: userIdInput.trim()
                                
                                val sessionManager = SessionManager(context)
                                sessionManager.saveSession(finalUserId, role.lowercase(), name.trim())
                                
                                Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                                delay(500)
                                navController.navigate("dashboard/${role.lowercase()}") {
                                    popUpTo("role_selection") { inclusive = false }
                                }
                            } else {
                                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                                Toast.makeText(context, "Signup Failed: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", color = TextMuted, fontSize = 14.sp)
                Text(
                    text = " Sign In",
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { navController.navigateUp() }
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

