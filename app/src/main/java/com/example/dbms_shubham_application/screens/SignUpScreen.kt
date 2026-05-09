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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.network.RetrofitClient
import com.example.dbms_shubham_application.ui.components.ModernTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Get colors from theme
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // --- LUXURY BACKGROUND GRADIENT ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.05f),
                            backgroundColor,
                            secondaryColor.copy(alpha = 0.05f)
                        )
                    )
                )
        )

        // Decorative background elements
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = 180.dp, y = (-120).dp)
                .background(
                    Brush.radialGradient(
                        listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Back Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(onBackground.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enterprise-grade Logo Container
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        )
                    )
                    .padding(2.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(backgroundColor)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(primaryColor, secondaryColor))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create Account",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = onBackground
            )
            
            Text(
                text = "Register as ${role.replaceFirstChar { it.uppercase() }} to access VJTI Portal",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = onBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 40.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                ModernTextField(
                    value = userIdInput,
                    onValueChange = { userIdInput = it },
                    label = if (role.lowercase() == "student") "Registration Number" else "Employee ID",
                    icon = Icons.Default.Badge,
                    colors = primaryColor to outlineColor,
                    textColor = onBackground,
                    surfaceColor = surfaceColor
                )

                ModernTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    icon = Icons.Default.Person,
                    colors = primaryColor to outlineColor,
                    textColor = onBackground,
                    surfaceColor = surfaceColor
                )

                ModernTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Institutional Email",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    colors = primaryColor to outlineColor,
                    textColor = onBackground,
                    surfaceColor = surfaceColor
                )

                ModernTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    icon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible },
                    colors = primaryColor to outlineColor,
                    textColor = onBackground,
                    surfaceColor = surfaceColor
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

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
                            if (response.isSuccessful && response.body()?.success == true) {
                                val body = response.body()!!
                                val finalUserId = body.user_id ?: userIdInput.trim()
                                
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
                    .height(60.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(role.lowercase() == "student") secondaryColor else primaryColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                } else {
                    Text("Register Now", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text("Already have an account?", color = onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
                Text(
                    text = " Sign In",
                    color = primaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { navController.navigateUp() }
                )
            }
        }
    }
}
