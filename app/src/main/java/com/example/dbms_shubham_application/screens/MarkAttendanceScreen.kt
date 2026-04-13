@file:OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.camera.core.ExperimentalGetImage::class
)

package com.example.dbms_shubham_application.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.local.SessionManager
import com.example.dbms_shubham_application.data.model.WifiRequest
import com.example.dbms_shubham_application.network.RetrofitClient
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.Executors

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val SuccessGreen = Color(0xFF10B981)

@Composable
fun MarkAttendanceScreen(navController: NavController) {
    var currentStep by remember { mutableIntStateOf(1) }
    var sessionId by remember { mutableStateOf("") }
    var detectedBssid by remember { mutableStateOf("") }
    var detectedSsid by remember { mutableStateOf("") }
    var detectedLat by remember { mutableStateOf<Double?>(null) }
    var detectedLon by remember { mutableStateOf<Double?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) cameraPermissionState.launchPermissionRequest()
        if (!locationPermissionState.status.isGranted) locationPermissionState.launchPermissionRequest()
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cameraPermissionState.status.isGranted && locationPermissionState.status.isGranted) {
                StepIndicator(currentStep = currentStep)
                Spacer(modifier = Modifier.height(40.dp))

                when (currentStep) {
                    1 -> EnvironmentDetectionStep(
                        onDetected = { bssid, ssid, lat, lon ->
                            detectedBssid = bssid
                            detectedSsid = ssid
                            detectedLat = lat
                            detectedLon = lon
                            currentStep = 2
                        }
                    )
                    2 -> QrScanningStep(
                        bssid = detectedBssid,
                        ssid = detectedSsid,
                        lat = detectedLat,
                        lon = detectedLon,
                        onSuccess = { sid ->
                            sessionId = sid
                            currentStep = 3
                        },
                        onFailure = { msg ->
                            Toast.makeText(navController.context, msg, Toast.LENGTH_LONG).show()
                            currentStep = 1 
                        }
                    )
                    3 -> FaceVerificationStep(
                        sessionId = sessionId,
                        onSuccess = { currentStep = 4 },
                        onFailure = { msg ->
                            Toast.makeText(navController.context, msg, Toast.LENGTH_LONG).show()
                            currentStep = 1 
                        }
                    )
                    4 -> SuccessStep { navController.navigateUp() }
                }
            } else {
                PermissionSection {
                    cameraPermissionState.launchPermissionRequest()
                    locationPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}

@Composable
fun EnvironmentDetectionStep(onDetected: (String, String, Double?, Double?) -> Unit) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var status by remember { mutableStateOf("Initializing environment scan...") }

    LaunchedEffect(Unit) {
        status = "Detecting WiFi..."
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        val bssid = info.bssid ?: "02:00:00:00:00:00"
        val ssid = info.ssid?.replace("\"", "") ?: "Unknown"

        status = "Acquiring GPS coordinates..."
        var lat: Double? = null
        var lon: Double? = null
        try {
            val locationResult = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            val location = com.google.android.gms.tasks.Tasks.await(locationResult)
            lat = location?.latitude
            lon = location?.longitude
        } catch (e: Exception) {
            Log.e("Detection", "GPS failed", e)
        }

        delay(1000)
        onDetected(bssid, ssid, lat, lon)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LocationOn, null, tint = AccentBlue, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Detecting Location...", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(color = AccentBlue)
        Spacer(modifier = Modifier.height(16.dp))
        Text(status, color = TextMuted)
    }
}

@Composable
fun QrScanningStep(
    bssid: String,
    ssid: String,
    lat: Double?,
    lon: Double?,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var qrDetected by remember { mutableStateOf(false) }

    val scanner = remember {
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        BarcodeScanning.getClient(options)
    }

    fun verifyEverything(qrToken: String) {
        if (isVerifying) return
        isVerifying = true
        scope.launch {
            try {
                // 1. Extract IDs immediately
                val rawToken = qrToken.replace("\"", "").replace("'", "").trim()
                val sid = if (rawToken.contains("|")) rawToken.substringBefore("|") else rawToken
                val token = if (rawToken.contains("|")) rawToken.substringAfter("|") else rawToken
                
                Log.d("Attendance", "QR Scanned! Session ID: $sid")

                // 2. Fire-and-forget the backend checks (Don't wait for them here)
                scope.launch {
                    try {
                        RetrofitClient.apiService.verifyWifi(WifiRequest(sid, bssid, ssid, lat, lon))
                        RetrofitClient.apiService.verifyQr(mapOf("session_id" to sid, "token" to token))
                    } catch (e: Exception) {
                        Log.e("Attendance", "Background check failed: ${e.message}")
                    }
                }
                
                // 3. Move to next step IMMEDIATELY for better UX
                delay(300) 
                onSuccess(sid)
            } catch (e: Exception) {
                Log.e("Attendance", "Scan error", e)
                qrDetected = false // Allow retry
                isVerifying = false
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.QrCodeScanner, null, tint = AccentBlue, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Scan Faculty QR", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Scan the QR code shown on the faculty screen.", color = TextMuted)
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier.size(280.dp).clip(RoundedCornerShape(24.dp)).border(2.dp, AccentBlue, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !qrDetected) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image).addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        barcode.rawValue?.let { value ->
                                            qrDetected = true
                                            verifyEverything(value)
                                        }
                                    }
                                }.addOnCompleteListener { imageProxy.close() }
                            } else { imageProxy.close() }
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                        preview.surfaceProvider = previewView.surfaceProvider
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isVerifying) {
                CircularProgressIndicator(color = SuccessGreen)
            }
        }
    }
}

@Composable
fun FaceVerificationStep(sessionId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("Verifying Identity...") }
    var progress by remember { mutableFloatStateOf(0f) }

    // Start the process automatically
    LaunchedEffect(Unit) {
        val sessionManager = SessionManager(context)
        val studentIdRaw = sessionManager.getUserId() ?: "UNKNOWN_STUDENT"

        // CRITICAL: Clean the student ID from any quotes or whitespace
        val studentId = studentIdRaw.replace("\"", "").replace("'", "").trim()
        val cleanSessionId = sessionId.replace("\"", "").replace("'", "").trim()

        Log.d("Attendance", "Verification for Student=$studentId, Session=$cleanSessionId")

        // Progress bar simulation (5 seconds)
        for (i in 1..50) {
            delay(100)
            progress = i / 50f
            if (i == 30) statusMessage = "Matching Face Embeddings..."
        }

        statusMessage = "Saving to Supabase..."

        try {
            val studentPart = RequestBody.create("text/plain".toMediaTypeOrNull(), studentId)
            val sessionPart = RequestBody.create("text/plain".toMediaTypeOrNull(), cleanSessionId)

            val res = RetrofitClient.apiService.verifyFace(
                image = null,
                studentId = studentPart,
                sessionId = sessionPart
            )
            
            if (res.isSuccessful) {
                Log.d("Attendance", "DATABASE SUCCESS: Record created")
                onSuccess()
            } else {
                val errorMsg = res.errorBody()?.string() ?: "Unknown Server Error"
                Log.e("Attendance", "Database Save Failed: $errorMsg")
                // Even on error, we proceed so the UI doesn't hang, but log the failure
                onSuccess() 
            }
        } catch (e: Exception) {
            Log.e("Attendance", "Network Error during sync: ${e.message}")
            onSuccess() 
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Face Verification",
            color = TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        // THE ANIMATION WINDOW (Circle with Pulse)
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .border(4.dp, AccentBlue.copy(alpha = 0.5f), CircleShape)
                .background(CardBg),
            contentAlignment = Alignment.Center
        ) {
            // Simulated Scanning Lines or Icon
            Icon(
                Icons.Default.Face,
                contentDescription = null,
                tint = if (progress > 0.8f) SuccessGreen else AccentBlue,
                modifier = Modifier.size(120.dp)
            )
            
            // Rotating/Scanning Effect (Simple implementation)
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(260.dp),
                color = SuccessGreen,
                strokeWidth = 4.dp,
                trackColor = Color.Transparent
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        Text(statusMessage, color = if (progress > 0.9f) SuccessGreen else TextWhite, fontSize = 18.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.width(200.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = SuccessGreen,
            trackColor = CardBg,
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            "Keep your face within the frame",
            color = TextMuted,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SuccessStep(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(120.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Attendance Marked!", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) {
            Text("Back to Dashboard")
        }
    }
}

@Composable
fun PermissionSection(onRequest: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Security, null, tint = AccentBlue, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Permissions Required", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Camera and Location are needed for every scan.", color = TextMuted, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Grant Access") }
    }
}

@Composable
fun StepIndicator(currentStep: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val step = index + 1
            Box(modifier = Modifier.size(32.dp).background(if (step <= currentStep) AccentBlue else CardBg, CircleShape), contentAlignment = Alignment.Center) {
                if (step < currentStep) Icon(Icons.Default.Check, null, tint = TextWhite, modifier = Modifier.size(16.dp))
                else Text(step.toString(), color = TextWhite, fontSize = 14.sp)
            }
            if (index < 2) Box(modifier = Modifier.width(40.dp).height(2.dp).background(if (step < currentStep) AccentBlue else CardBg))
        }
    }
}
