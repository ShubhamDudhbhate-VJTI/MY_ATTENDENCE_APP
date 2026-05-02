package com.example.dbms_shubham_application.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.DepartmentAnalytics
import com.example.dbms_shubham_application.data.model.TrendData
import com.example.dbms_shubham_application.utils.DateTimeUtils
import com.example.dbms_shubham_application.utils.FileUtils
import com.example.dbms_shubham_application.ui.components.DatePickerField
import com.example.dbms_shubham_application.ui.components.FilterDropdown
import com.example.dbms_shubham_application.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HODAnalyticsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val userId = sessionManager.getUserId() ?: ""
    var departmentId by remember { mutableStateOf("IT") } // Default to IT, will update from profile
    val colorScheme = MaterialTheme.colorScheme

    var visible by remember { mutableStateOf(false) }
    var itemsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { 
        visible = true
        kotlinx.coroutines.delay(300)
        itemsVisible = true
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Analytics Data State
    var analyticsData by remember { mutableStateOf<DepartmentAnalytics?>(null) }

    // Dropdown Data
    val branches = listOf("All", "IT", "Computer", "Mechanical", "Civil", "ENTC")
    val years = listOf("All", "First Year", "Second Year", "Third Year", "Final Year")
    var facultyList by remember { mutableStateOf<List<com.example.dbms_shubham_application.data.model.UserProfile>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<com.example.dbms_shubham_application.data.model.Subject>>(emptyList()) }

    // Filter States
    var selectedBranch by remember { mutableStateOf("All") }
    var selectedYear by remember { mutableStateOf("All") }
    var selectedFacultyId by remember { mutableStateOf("All") }
    var selectedSubjectId by remember { mutableStateOf("All") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    // Data Fetching Function
    val fetchAnalytics = {
        scope.launch {
            isRefreshing = true
            try {
                val response = RetrofitClient.apiService.getDepartmentAnalytics(departmentId)
                if (response.isSuccessful) {
                    analyticsData = response.body()
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { isRefreshing = false }
        }
    }

    // Initial Data Fetch
    LaunchedEffect(Unit) {
        try {
            // 1. Get User Profile to find actual Department
            val profileResponse = RetrofitClient.apiService.getUserProfile(userId)
            if (profileResponse.isSuccessful) {
                val profile = profileResponse.body()
                val dept = profile?.academic?.get("department")
                if (!dept.isNullOrEmpty()) {
                    departmentId = dept
                }
            }
            
            fetchAnalytics()

            val facResponse = RetrofitClient.apiService.getAllFaculty()
            if (facResponse.isSuccessful) facultyList = facResponse.body() ?: emptyList()
            
            val subResponse = RetrofitClient.apiService.getSubjects()
            if (subResponse.isSuccessful) subjects = subResponse.body() ?: emptyList()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun downloadHodReport(isExcel: Boolean = false) {
        isDownloading = true
        scope.launch(Dispatchers.IO) {
            try {
                val response = if (isExcel) {
                    RetrofitClient.apiService.downloadHodMasterExcel(
                        departmentId = departmentId,
                        facultyId = selectedFacultyId.takeIf { it != "All" },
                        branch = selectedBranch.takeIf { it != "All" },
                        year = selectedYear.takeIf { it != "All" },
                        subjectId = selectedSubjectId.takeIf { it != "All" },
                        startDate = startDate.takeIf { it.isNotEmpty() },
                        endDate = endDate.takeIf { it.isNotEmpty() }
                    )
                } else {
                    RetrofitClient.apiService.downloadHodMasterPdf(
                        departmentId = departmentId,
                        facultyId = selectedFacultyId.takeIf { it != "All" },
                        branch = selectedBranch.takeIf { it != "All" },
                        year = selectedYear.takeIf { it != "All" },
                        subjectId = selectedSubjectId.takeIf { it != "All" },
                        startDate = startDate.takeIf { it.isNotEmpty() },
                        endDate = endDate.takeIf { it.isNotEmpty() }
                    )
                }
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val ext = if (isExcel) "csv" else "pdf"
                        val mimeType = if (isExcel) "text/csv" else "application/pdf"
                        FileUtils.saveFile(body.byteStream(), "HOD_Department_Report.$ext", mimeType, context)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No records found for these filters", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) { isDownloading = false }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
        title = { 
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -20 }, animationSpec = tween(600))
                    ) {
                        Column {
                            Text("Department Analytics", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("Department: $departmentId", color = colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                            .background(colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                            .border(1.dp, colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colorScheme.onBackground, modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { fetchAnalytics() },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .background(colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Modern Tab Selector
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 100)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, 100))
            ) {
                Surface(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    color = colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        listOf("Live Dashboard", "Master Reports").forEachIndexed { index, title ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        if (selectedTab == index) colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTab = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    color = if (selectedTab == index) colorScheme.onPrimary else colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // DASHBOARD VIEW
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        AnimatedVisibility(
                            visible = itemsVisible,
                            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(600))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Real-time Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(12.dp))
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF4CAF50), CircleShape))
                                }
                            }
                        }
                    }
                    
                    item {
                        AnimatedVisibility(
                            visible = itemsVisible,
                            enter = fadeIn(tween(600, 100)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(600, 100))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AnalyticsStatCard(
                                    StatItem("Avg. Attendance", analyticsData?.avg_attendance ?: "...", Icons.AutoMirrored.Filled.TrendingUp, colorScheme.primary),
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsStatCard(
                                    StatItem("Faculty Count", analyticsData?.total_faculty?.toString() ?: "...", Icons.Default.People, colorScheme.tertiary),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = itemsVisible,
                            enter = fadeIn(tween(600, 200)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(600, 200))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AnalyticsStatCard(
                                    StatItem("Total Classes", analyticsData?.total_classes?.toString() ?: "...", Icons.Default.BarChart, colorScheme.secondary),
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsStatCard(
                                    StatItem("Defaulters", analyticsData?.defaulter_count?.toString() ?: "...", Icons.AutoMirrored.Filled.TrendingDown, colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    item { 
                        AnimatedVisibility(
                            visible = itemsVisible,
                            enter = fadeIn(tween(600, 300)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(600, 300))
                        ) {
                            if (analyticsData == null && !isRefreshing) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ErrorOutline, null, tint = colorScheme.error)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("No data available for this department.", color = colorScheme.onErrorContainer, fontWeight = FontWeight.Medium)
                                    }
                                }
                            } else {
                                DynamicTrendChart(trends = analyticsData?.trends ?: emptyList())
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = itemsVisible,
                            enter = fadeIn(tween(600, 400)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600, 400))
                        ) {
                            PremiumDepartmentBadge(departmentId)
                        }
                    }
                }
            } else {
                // REPORT FILTERS VIEW
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(600, 100)) + slideInHorizontally(initialOffsetX = { -30 }, animationSpec = tween(600, 100))
                    ) {
                        Column {
                            Text("Audit Parameters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text("Filter criteria for generating master academic reports.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colorScheme.outline.copy(alpha = 0.1f))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val filterDelay = 200
                        
                        // Branch Filter
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, filterDelay)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, filterDelay))
                        ) {
                            FilterDropdown(label = "Branch", options = branches, selected = selectedBranch) { selectedBranch = it }
                        }

                        // Academic Year Filter
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, filterDelay + 100)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, filterDelay + 100))
                        ) {
                            FilterDropdown(label = "Academic Year", options = years, selected = selectedYear) { selectedYear = it }
                        }

                        // Faculty Filter
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, filterDelay + 200)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, filterDelay + 200))
                        ) {
                            val facultyNames = listOf("All Faculty") + facultyList.map { it.full_name }
                            FilterDropdown(
                                label = "Faculty Member", 
                                options = facultyNames, 
                                selected = facultyList.find { it.id == selectedFacultyId }?.full_name ?: "All Faculty"
                            ) { name ->
                                selectedFacultyId = if (name == "All Faculty") "All" else facultyList.find { it.full_name == name }?.id ?: "All"
                            }
                        }

                        // Subject Filter
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, filterDelay + 300)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, filterDelay + 300))
                        ) {
                            val subjectNames = listOf("All Subjects") + subjects.map { it.name }
                            FilterDropdown(
                                label = "Subject", 
                                options = subjectNames, 
                                selected = subjects.find { it.id == selectedSubjectId }?.name ?: "All Subjects"
                            ) { name ->
                                selectedSubjectId = if (name == "All Subjects") "All" else subjects.find { it.name == name }?.id ?: "All"
                            }
                        }

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, 600)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, 600))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DatePickerField(label = "Start Date", value = startDate, modifier = Modifier.weight(1f)) { startDate = it }
                                DatePickerField(label = "End Date", value = endDate, modifier = Modifier.weight(1f)) { endDate = it }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(600, 700)) + scaleIn(animationSpec = tween(600, 700))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { downloadHodReport(isExcel = false) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isDownloading,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Generate Master PDF Report", fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            OutlinedButton(
                                onClick = { downloadHodReport(isExcel = true) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isDownloading,
                                border = BorderStroke(2.dp, colorScheme.primary)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text("Export Data to Excel/CSV", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun PremiumDepartmentBadge(dept: String) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.primary),
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(
            Brush.horizontalGradient(
                listOf(colorScheme.primary, colorScheme.tertiary)
            )
        )) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Domain, null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Official Department Head", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text("$dept Engineering Department", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DynamicTrendChart(trends: List<TrendData>) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Attendance Trends", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Monthly Average %", fontSize = 12.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (trends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    trends.forEach { data ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${data.value}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .fillMaxHeight(data.value.toFloat() / 100f)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            if (data.value < 75) listOf(colorScheme.error, colorScheme.error.copy(alpha = 0.6f))
                                            else listOf(colorScheme.primary, colorScheme.primary.copy(alpha = 0.6f))
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(data.month.take(3), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsStatCard(stat: StatItem, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(44.dp).background(stat.color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(stat.icon, null, tint = stat.color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(stat.title, fontSize = 11.sp, color = colorScheme.onSurface.copy(alpha = 0.6f))
                Text(stat.value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colorScheme.onSurface)
            }
        }
    }
}

data class StatItem(val title: String, val value: String, val icon: ImageVector, val color: Color)
