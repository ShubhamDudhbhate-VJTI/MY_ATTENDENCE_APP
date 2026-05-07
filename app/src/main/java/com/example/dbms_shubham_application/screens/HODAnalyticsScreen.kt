package com.example.dbms_shubham_application.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.example.dbms_shubham_application.ui.components.DynamicTrendChart
import com.example.dbms_shubham_application.ui.components.ProfessionalStatStrip
import com.example.dbms_shubham_application.ui.components.StatStripItem
import com.example.dbms_shubham_application.ui.components.VerticalStripDivider
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
    var departmentId by remember { mutableStateOf("IT") }
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
    val branches = listOf("All", "Information Technology", "Computer Engineering", "Mechanical Engineering", "Civil Engineering", "EXTC Engineering")
    val years = listOf("All", "First Year", "Second Year", "Third Year", "Fourth Year")
    var facultyList by remember { mutableStateOf<List<com.example.dbms_shubham_application.data.model.UserProfile>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<com.example.dbms_shubham_application.data.model.Subject>>(emptyList()) }

    // Filter States
    var selectedBranch by remember { mutableStateOf("All") }
    var selectedYear by remember { mutableStateOf("All") }
    var selectedFacultyId by remember { mutableStateOf("All") }
    var selectedSubjectId by remember { mutableStateOf("All") }
    var studentRollNo by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    // Summary State
    var summaryCount by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isLoadingSummary by remember { mutableStateOf(false) }

    val fetchSummary = {
        scope.launch {
            isLoadingSummary = true
            try {
                val response = RetrofitClient.apiService.getReportsSummary(
                    departmentId = departmentId,
                    facultyId = selectedFacultyId.takeIf { it != "All" },
                    branch = selectedBranch.takeIf { it != "All" },
                    year = selectedYear.takeIf { it != "All" },
                    subjectId = selectedSubjectId.takeIf { it != "All" },
                    studentId = studentRollNo.takeIf { it.isNotEmpty() },
                    startDate = startDate.takeIf { it.isNotEmpty() },
                    endDate = endDate.takeIf { it.isNotEmpty() }
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    summaryCount = (body?.get("total_sessions") ?: 0) to (body?.get("total_students") ?: 0)
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { isLoadingSummary = false }
        }
    }

    LaunchedEffect(selectedBranch, selectedYear, selectedFacultyId, selectedSubjectId, studentRollNo, startDate, endDate) {
        if (selectedTab == 1) fetchSummary()
    }

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

    LaunchedEffect(Unit) {
        try {
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
            if (facResponse.isSuccessful) {
                // Filter out fake faculty
                facultyList = (facResponse.body() ?: emptyList()).filter { !it.id.startsWith("cloud___") }
            }
            
            val subResponse = RetrofitClient.apiService.getSubjects()
            if (subResponse.isSuccessful) subjects = subResponse.body() ?: emptyList()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun downloadHodReport(isExcel: Boolean = false) {
        isDownloading = true
        scope.launch(Dispatchers.IO) {
            try {
                // Ensure dates are formatted correctly or passed as is if already YYYY-MM-DD
                val sDate = startDate.takeIf { it.isNotEmpty() }
                val eDate = endDate.takeIf { it.isNotEmpty() }
                
                val response = if (isExcel) {
                    RetrofitClient.apiService.downloadHodMasterExcel(
                        departmentId = departmentId,
                        facultyId = selectedFacultyId.takeIf { it != "All" },
                        branch = selectedBranch.takeIf { it != "All" },
                        year = selectedYear.takeIf { it != "All" },
                        subjectId = selectedSubjectId.takeIf { it != "All" },
                        startDate = sDate,
                        endDate = eDate,
                        studentId = studentRollNo.takeIf { it.isNotEmpty() }
                    )
                } else {
                    RetrofitClient.apiService.downloadHodMasterPdf(
                        departmentId = departmentId,
                        facultyId = selectedFacultyId.takeIf { it != "All" },
                        branch = selectedBranch.takeIf { it != "All" },
                        year = selectedYear.takeIf { it != "All" },
                        subjectId = selectedSubjectId.takeIf { it != "All" },
                        startDate = sDate,
                        endDate = eDate,
                        studentId = studentRollNo.takeIf { it.isNotEmpty() }
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

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        // Advanced Decorative Background
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

            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to colorScheme.tertiary.copy(0.06f),
                    1.0f to Color.Transparent
                ),
                radius = 450.dp.toPx(),
                center = Offset(width * 0.8f, height * 0.9f)
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -20 }, animationSpec = tween(600))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(end = 48.dp)) {
                                Text(
                                    "Academic Attendance System",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text("Department Dashboard", color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.5).sp)
                                Text("Domain: $departmentId Engineering", color = colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
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
                                .border(1.dp, colorScheme.outline.copy(alpha = 0.1f), CircleShape)
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
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // Glassmorphic Tab Switcher
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600, 100)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, 100))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colorScheme.surfaceVariant.copy(0.4f))
                            .padding(4.dp)
                    ) {
                        val transition = updateTransition(selectedTab, label = "TabTransition")
                        val indicatorOffset by transition.animateDp(label = "IndicatorOffset") { if (it == 0) 0.dp else 165.dp }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .padding(horizontal = 2.dp)
                                .offset(x = indicatorOffset)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .background(colorScheme.primary, RoundedCornerShape(16.dp))
                        )

                        Row(modifier = Modifier.fillMaxSize()) {
                            listOf("Dashboard", "Reports Center").forEachIndexed { index, title ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { selectedTab = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        title,
                                        color = if (selectedTab == index) colorScheme.onPrimary else colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedContent(targetState = selectedTab, transitionSpec = { fadeIn() togetherWith fadeOut() }) { targetTab ->
                    if (targetTab == 0) {
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
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        ProfessionalStatStrip {
                                            StatStripItem(
                                                label = "Avg. Attendance",
                                                value = analyticsData?.avg_attendance ?: "...",
                                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                                color = colorScheme.primary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            VerticalStripDivider()
                                            StatStripItem(
                                                label = "Faculty",
                                                value = analyticsData?.total_faculty?.toString() ?: "...",
                                                icon = Icons.Default.People,
                                                color = colorScheme.tertiary,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        ProfessionalStatStrip {
                                            StatStripItem(
                                                label = "Total Classes",
                                                value = analyticsData?.total_classes?.toString() ?: "...",
                                                icon = Icons.Default.BarChart,
                                                color = colorScheme.secondary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            VerticalStripDivider()
                                            StatStripItem(
                                                label = "Defaulters",
                                                value = analyticsData?.defaulter_count?.toString() ?: "...",
                                                icon = Icons.AutoMirrored.Filled.TrendingDown,
                                                color = colorScheme.error,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
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
                        // REPORT FILTERS VIEW (Reports Center)
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
                                    Text("Report Parameters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                    Text("Filter criteria for generating master academic reports.", color = Color.Gray, fontSize = 14.sp)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colorScheme.outline.copy(alpha = 0.1f))

                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                val filterDelay = 200
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(600, filterDelay)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, filterDelay))
                                ) {
                                    FilterDropdown(label = "Branch", options = branches, selected = selectedBranch) { selectedBranch = it }
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(600, filterDelay + 100)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, filterDelay + 100))
                                ) {
                                    FilterDropdown(label = "Academic Year", options = years, selected = selectedYear) { selectedYear = it }
                                }

                                val facultyNames = listOf("All Faculty") + facultyList.map { it.full_name }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(600, filterDelay + 200)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, filterDelay + 200))
                                ) {
                                    FilterDropdown(
                                        label = "Faculty Member", 
                                        options = facultyNames, 
                                        selected = facultyList.find { it.id == selectedFacultyId }?.full_name ?: "All Faculty"
                                    ) { name ->
                                        selectedFacultyId = if (name == "All Faculty") "All" else facultyList.find { it.full_name == name }?.id ?: "All"
                                    }
                                }

                                val subjectNames = listOf("All Subjects") + subjects.map { it.name }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(600, filterDelay + 300)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, filterDelay + 300))
                                ) {
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
                                    enter = fadeIn(tween(600, filterDelay + 350)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, filterDelay + 350))
                                ) {
                                    OutlinedTextField(
                                        value = studentRollNo,
                                        onValueChange = { studentRollNo = it },
                                        label = { Text("Student Roll No (Optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        leadingIcon = { Icon(Icons.Default.PersonSearch, null) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colorScheme.primary,
                                            unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    )
                                }

                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(600, 600)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, 600))
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        DatePickerField(label = "Start Date", value = startDate, modifier = Modifier.weight(1f)) { 
                                            startDate = it 
                                        }
                                        DatePickerField(label = "End Date", value = endDate, modifier = Modifier.weight(1f)) { 
                                            endDate = it 
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(600, 650)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, 650))
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colorScheme.primary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SummaryItem("Sessions", summaryCount?.first?.toString() ?: "0", Icons.Default.Event, isLoadingSummary)
                                        VerticalDivider(modifier = Modifier.height(30.dp), color = colorScheme.primary.copy(alpha = 0.2f))
                                        SummaryItem("Students", summaryCount?.second?.toString() ?: "0", Icons.Default.Groups, isLoadingSummary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

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
fun AnalyticsStatCard(stat: StatItem, 
    modifier: Modifier = Modifier) {
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

@Composable
fun SummaryItem(label: String, value: String, icon: ImageVector, isLoading: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colorScheme.primary.copy(alpha = 0.8f))
        }
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(top = 8.dp), strokeWidth = 2.dp)
        } else {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = colorScheme.onSurface)
        }
    }
}

data class StatItem(val title: String, val value: String, val icon: ImageVector, val color: Color)
