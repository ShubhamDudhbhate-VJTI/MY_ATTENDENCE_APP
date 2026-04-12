package com.example.dbms_shubham_application.screens

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.dbms_shubham_application.data.model.QrRequest
import com.example.dbms_shubham_application.data.model.WifiRequest
import com.example.dbms_shubham_application.network.RetrofitClient
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.Executors

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)
private val SuccessGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MarkAttendanceScreen(navController: NavController) {
    var currentStep by remember { mutableStateOf(1) } // 1: WiFi, 2: QR, 3: Face, 4: Success
    val scope = rememberCoroutineScope()
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Attendance Flow", color = TextWhite, fontWeight = FontWeight.Bold) },
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
            if (cameraPermissionState.status.isGranted) {
                StepIndicator(currentStep = currentStep)
                Spacer(modifier = Modifier.height(40.dp))

                when (currentStep) {
                    1 -> WifiDetectionStep { currentStep = 2 }
                    2 -> QrScanningStep { currentStep = 3 }
                    3 -> FaceVerificationStep { currentStep = 4 }
                    4 -> SuccessStep { navController.navigateUp() }
                }
            } else {
                CameraPermissionSection(
                    shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
        }
    }
}

@Composable
fun CameraPermissionSection(shouldShowRationale: Boolean, onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CameraAlt, null, tint = AccentBlue, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Camera Permission Required", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (shouldShowRationale) {
                "The camera is needed for QR scanning and face verification. Please grant permission to continue."
            } else {
                "This feature requires camera access to scan QR codes and verify your identity."
            },
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val step = index + 1
            val isActive = step <= currentStep
            val isCompleted = step < currentStep

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(if (isActive) AccentBlue else CardBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, null, tint = TextWhite, modifier = Modifier.size(16.dp))
                } else {
                    Text(step.toString(), color = if (isActive) TextWhite else TextMuted, fontSize = 14.sp)
                }
            }

            if (index < 2) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(if (step < currentStep) AccentBlue else CardBg)
                )
            }
        }
    }
}

@Composable
fun WifiDetectionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    var isVerifying by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready to verify WiFi connection") }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun verifyWifi() {
        isVerifying = true
        statusMessage = "Detecting classroom WiFi..."
        scope.launch {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wifiManager.connectionInfo
                val bssid = info.bssid ?: "02:00:00:00:00:00"
                val ssid = info.ssid ?: "Unknown"

                val response = RetrofitClient.apiService.verifyWifi(WifiRequest(bssid, ssid))
                if (response.isSuccessful && response.body()?.success == true) {
                    isSuccess = true
                    statusMessage = "Verified: ${response.body()?.classroomName}"
                } else {
                    statusMessage = response.body()?.message ?: "WiFi verification failed"
                }
            } catch (e: Exception) {
                statusMessage = "Error: ${e.message}"
            } finally {
                isVerifying = false
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Wifi, null, tint = AccentBlue, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Step 1: WiFi Verification", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(statusMessage, color = if (isSuccess) SuccessGreen else TextMuted, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(40.dp))
        if (isVerifying) {
            CircularProgressIndicator(color = AccentBlue)
        } else if (!isSuccess) {
            Button(
                onClick = { verifyWifi() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Scan & Verify WiFi")
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Proceed to QR Scan")
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanningStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var qrDetected by remember { mutableStateOf(false) }

    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    fun verifyQrToken(token: String) {
        if (isVerifying) return
        isVerifying = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.verifyQr(QrRequest(token, "SESSION_101"))
                if (response.isSuccessful && response.body()?.get("success") == true) {
                    onNext()
                } else {
                    Toast.makeText(context, "Invalid QR Code", Toast.LENGTH_SHORT).show()
                    qrDetected = false
                    isVerifying = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                qrDetected = false
                isVerifying = false
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.QrCodeScanner, null, tint = AccentBlue, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Step 2: QR Scan", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Point camera at the classroom QR code", color = TextMuted, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(40.dp))
        
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, if (qrDetected) SuccessGreen else AccentBlue, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !qrDetected) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { value ->
                                                qrDetected = true
                                                verifyQrToken(value)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                        } catch (e: Exception) { Log.e("QR", "Camera binding failed", e) }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (isVerifying) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun FaceVerificationStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Position your face in the circle") }
    
    val imageCapture = remember { ImageCapture.Builder().build() }

    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "scanLine"
    )

    fun captureAndVerify() {
        isProcessing = true
        statusMessage = "Capturing face..."
        val photoFile = File(context.cacheDir, "face_capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    statusMessage = "Verifying with AI..."
                    scope.launch {
                        try {
                            val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val body = MultipartBody.Part.createFormData("image", photoFile.name, requestFile)
                            val response = RetrofitClient.apiService.verifyFace(body, "ST123")
                            if (response.isSuccessful && response.body()?.success == true) {
                                onNext()
                            } else {
                                statusMessage = response.body()?.message ?: "Face mismatch"
                                isProcessing = false
                            }
                        } catch (e: Exception) {
                            statusMessage = "Error: ${e.message}"
                            isProcessing = false
                        }
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    statusMessage = "Capture failed: ${exception.message}"
                    isProcessing = false
                }
            }
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Step 3: Face Verification", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(260.dp).clip(CircleShape).background(Color.Black).border(2.dp, if (isProcessing) SuccessGreen else AccentBlue, CircleShape)) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build()
                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                                preview.setSurfaceProvider(previewView.surfaceProvider)
                            } catch (e: Exception) { Log.e("Face", "Camera binding failed", e) }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isProcessing) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y = size.height * scanLineOffset
                        drawLine(
                            brush = Brush.verticalGradient(listOf(Color.Transparent, AccentBlue, Color.Transparent)),
                            start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            }
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(290.dp), color = AccentBlue, strokeWidth = 4.dp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(statusMessage, color = TextMuted, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!isProcessing) {
            Button(
                onClick = { captureAndVerify() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Verify Identity")
            }
        }
    }
}

@Composable
fun SuccessStep(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(120.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Attendance Marked!", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Your attendance has been recorded successfully.", color = TextMuted, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Back to Dashboard")
        }
    }
}
