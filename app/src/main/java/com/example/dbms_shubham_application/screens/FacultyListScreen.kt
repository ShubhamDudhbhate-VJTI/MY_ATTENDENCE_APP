package com.example.dbms_shubham_application.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.ui.components.ModernTextField
import com.example.dbms_shubham_application.data.model.UserProfile
import com.example.dbms_shubham_application.network.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyListScreen(navController: NavController) {
    val colorScheme = MaterialTheme.colorScheme
    var facultyList by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        try {
            val response = RetrofitClient.apiService.getAllFaculty()
            if (response.isSuccessful) {
                facultyList = (response.body() ?: emptyList()).filter { !it.id.startsWith("cloud___") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val filteredList = facultyList.filter {
        it.full_name.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        // Cyber Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to colorScheme.primary.copy(0.12f),
                    1.0f to Color.Transparent
                ),
                radius = 600.dp.toPx(),
                center = Offset(width * 0.9f, height * 0.05f)
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to colorScheme.secondary.copy(0.08f),
                    1.0f to Color.Transparent
                ),
                radius = 500.dp.toPx(),
                center = Offset(width * 0.1f, height * 0.45f)
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(end = 48.dp)) {
                            Text(
                                "Veermata Jijabai Technological Institute",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text("Faculty Directory", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.5).sp)
                            Text("INSTITUTIONAL REGISTRY", color = colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.padding(8.dp).size(40.dp).background(colorScheme.surface.copy(alpha = 0.5f), CircleShape).border(1.dp, colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically()) {
                    ModernTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Institutional Registry Search",
                        placeholder = "ID, Name or Email...",
                        icon = Icons.Default.Search,
                        modifier = Modifier.padding(20.dp),
                        colors = colorScheme.primary to colorScheme.outline.copy(alpha = 0.2f)
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = colorScheme.primary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("ACCESSING REGISTRY...", style = MaterialTheme.typography.labelSmall, color = colorScheme.primary, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("NO MATCHING RECORDS", fontFamily = FontFamily.Monospace, color = colorScheme.onSurface.copy(0.4f), fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredList) { faculty ->
                            FacultyMemberCard(faculty)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FacultyMemberCard(faculty: UserProfile) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = colorScheme.primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.8f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(colorScheme.primary.copy(0.1f), colorScheme.secondary.copy(0.05f))
                        ), 
                        RoundedCornerShape(18.dp)
                    )
                    .border(1.dp, colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VerifiedUser, 
                    null, 
                    tint = colorScheme.primary, 
                    modifier = Modifier.size(30.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = faculty.full_name.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = faculty.email.lowercase(),
                    fontSize = 13.sp,
                    color = colorScheme.onSurface.copy(0.6f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.2).sp
                )
                
                Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val branch = faculty.academic?.get("branch") ?: "GEN"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "DEPT: $branch",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.secondary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = faculty.role.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = colorScheme.secondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .shadow(8.dp, CircleShape, spotColor = Color(0xFF4CAF50))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "ACTIVE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4CAF50),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

