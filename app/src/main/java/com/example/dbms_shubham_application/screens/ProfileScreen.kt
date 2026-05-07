package com.example.dbms_shubham_application.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.UserProfile
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

// Remove hardcoded color constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    isDark: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val rawUserId = sessionManager.getUserId() ?: ""
    val userId = rawUserId.replace("\"", "").trim()
    
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var isSavingName by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                isUploading = true
                try {
                    val file = File(context.cacheDir, "profile_temp.jpg")
                    context.contentResolver.openInputStream(it)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                    
                    val response = RetrofitClient.apiService.uploadProfilePhoto(userId, body)
                    if (response.isSuccessful) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                            retryTrigger++ // Refresh profile
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Upload failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isUploading = false
                }
            }
        }
    }

    LaunchedEffect(userId, retryTrigger) {
        if (userId.isNotEmpty()) {
            isLoading = true
            hasError = false
            try {
                val response = RetrofitClient.apiService.getUserProfile(userId)
                if (response.isSuccessful) {
                    userProfile = response.body()
                    newName = userProfile?.full_name ?: ""
                } else {
                    hasError = true
                }
            } catch (e: Exception) {
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }

    if (isEditingName) {
        AlertDialog(
            onDismissRequest = { if (!isSavingName) isEditingName = false },
            title = { Text("Edit Full Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    enabled = !isSavingName,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = !isSavingName && newName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            isSavingName = true
                            try {
                                val response = RetrofitClient.apiService.updateUser(userId, mapOf("full_name" to newName))
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Name updated!", Toast.LENGTH_SHORT).show()
                                    retryTrigger++
                                    isEditingName = false
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error updating name", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSavingName = false
                            }
                        }
                    }
                ) {
                    if (isSavingName) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isSavingName, onClick = { isEditingName = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (hasError) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Could not load profile", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Text("Check your internet or server connection", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { retryTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Try Again")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    item {
                        ProfileHeaderCard(
                            profile = userProfile,
                            isUploading = isUploading,
                            retryTrigger = retryTrigger,
                            onPhotoClick = { photoPickerLauncher.launch("image/*") },
                            onNameClick = { isEditingName = true }
                        )
                    }

                    item {
                        ProfileSectionTitle("Academic Information")
                        val academicItems = mutableListOf<Triple<ImageVector, String, String>>()
                        
                        userProfile?.let { profile ->
                            if (profile.role == "student") {
                                academicItems.add(Triple(Icons.Default.School, "Branch", profile.academic?.get("branch") ?: "Information Technology"))
                                academicItems.add(Triple(Icons.Default.Class, "Year", profile.academic?.get("year") ?: "S.Y. B.Tech"))
                                academicItems.add(Triple(Icons.Default.Fingerprint, "Roll Number", profile.academic?.get("reg_no") ?: userId))
                                academicItems.add(Triple(Icons.Default.Face, "Biometric Status", if (profile.image_url != null) "Verified & Active" else "Pending Enrollment"))
                            } else {
                                academicItems.add(Triple(Icons.Default.School, "Department", profile.academic?.get("branch") ?: "Information Technology"))
                                academicItems.add(Triple(Icons.Default.Work, "Designation", profile.academic?.get("designation") ?: "Faculty"))
                                academicItems.add(Triple(Icons.Default.Badge, "Employee ID", profile.academic?.get("employee_id") ?: userId))
                                academicItems.add(Triple(Icons.Default.Security, "System Access", "Authorized Faculty"))
                            }
                        }

                        if (academicItems.isEmpty()) {
                            academicItems.add(Triple(Icons.Default.Info, "Status", "No academic records found"))
                        }

                        ProfileInfoCard(academicItems)
                    }

                    item {
                        ProfileSectionTitle("Settings")
                        SettingsCard(navController, sessionManager, isDark, onThemeChange)
                    }

                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Version 1.0.0 (Stable)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderCard(
    profile: UserProfile?,
    isUploading: Boolean = false,
    retryTrigger: Int = 0,
    onPhotoClick: () -> Unit = {},
    onNameClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "profile_gradient")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "gradient_shift"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    start = androidx.compose.ui.geometry.Offset(gradientShift, 0f),
                    end = androidx.compose.ui.geometry.Offset(gradientShift + 600f, 600f)
                ),
                RoundedCornerShape(32.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPhotoClick() }
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = remember(profile, retryTrigger) {
                    when {
                        profile?.profile_photo_url != null -> RetrofitClient.getProfilePhotoUrl(profile.id)
                        profile?.image_url != null -> "${RetrofitClient.getBaseUrl()}${profile.image_url}"
                        else -> null
                    }
                }

                if (imageUrl != null) {
                    AsyncImage(
                        model = "$imageUrl?v=$retryTrigger",
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = (profile?.full_name?.take(1) ?: profile?.username?.take(1) ?: "U").uppercase(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (isUploading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                // Edit Icon Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Change Photo",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Verification Shield Badge
                if (profile?.image_url != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Face Registered",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNameClick() }
            ) {
                Text(
                    text = profile?.full_name ?: "User Name",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Name",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = profile?.email ?: "student@academic.edu",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileMiniStat("Role", profile?.role?.replaceFirstChar { it.uppercase() } ?: "User")
                VerticalDivider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ProfileMiniStat("Status", if (profile?.image_url != null) "Verified" else "Pending")
            }

            Spacer(modifier = Modifier.height(28.dp))
            
            // "Biometric ID Card" Branding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BIOMETRIC ID", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(profile?.username ?: "N/A", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ProfileInfoCard(items: List<Triple<ImageVector, String, String>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.first, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Column {
                        Text(item.second, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold)
                        Text(item.third, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
                if (index < items.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    navController: NavController, 
    sessionManager: SessionManager,
    isDark: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            SettingsRow(
                icon = Icons.Default.DarkMode, 
                title = "Dark Mode", 
                subtitle = "Switch between light and dark theme",
                trailing = {
                    Switch(
                        checked = isDark,
                        onCheckedChange = { onThemeChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                }
            )
            
            SettingsRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "App alerts and reminders",
                trailing = {
                    var enabled by remember { mutableStateOf(sessionManager.isNotificationsEnabled()) }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            sessionManager.setNotificationsEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                }
            )
            SettingsRow(
                icon = Icons.Default.Security,
                title = "Privacy & Security",
                subtitle = "Manage biometric data",
                onClick = {
                    Toast.makeText(context, "Biometric data is encrypted and stored securely.", Toast.LENGTH_LONG).show()
                }
            )
            SettingsRow(
                icon = Icons.Default.Help,
                title = "Help Center",
                subtitle = "FAQs and support",
                onClick = {
                    Toast.makeText(context, "Support: support@attendx.com", Toast.LENGTH_SHORT).show()
                }
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { 
                        sessionManager.logout()
                        navController.navigate("role_selection") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector, 
    title: String, 
    subtitle: String, 
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}
