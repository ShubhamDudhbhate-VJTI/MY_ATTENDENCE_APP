@file:OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.dbms_shubham_application.screens

import androidx.annotation.OptIn as AndroidxOptIn
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.wifi.WifiManager
import android.util.Log
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.dbms_shubham_application.ui.theme.FacebookBlue
import com.example.dbms_shubham_application.ui.theme.RedAccent
import com.example.dbms_shubham_application.ui.theme.YellowAccent
import com.example.dbms_shubham_application.utils.DateTimeUtils
import com.example.dbms_shubham_application.data.model.SessionDetailsResponse
import java.util.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

// Removed hardcoded colors, using MaterialTheme.colorScheme instead

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cameraPermissionState.status.isGranted && locationPermissionState.status.isGranted) {
                StepIndicator(currentStep = currentStep)
                Spacer(modifier = Modifier.height(40.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }.using(
                            SizeTransform(clip = false)
                        )
                    }, label = "step_transition"
                ) { targetStep ->
                    when (targetStep) {
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
                            latitude = detectedLat,
                            longitude = detectedLon,
                            onSuccess = { currentStep = 4 },
                            onFailure = { msg ->
                                if (msg.contains("Network") || msg.contains("Session")) {
                                    Toast.makeText(navController.context, msg, Toast.LENGTH_LONG).show()
                                    currentStep = 1
                                } else {
                                    Log.d("Attendance", "Face error: $msg")
                                }
                            }
                        )
                        4 -> SuccessStep(sessionId = sessionId) { navController.navigateUp() }
                    }
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
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        status = "Detecting WiFi..."
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        val bssid = info.bssid ?: "02:00:00:00:00:00"
        val ssid = info.ssid?.replace("\"", "") ?: "Unknown"

        status = "Verifying institutional location..."
        var lat: Double? = null
        var lon: Double? = null
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val locationResult = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                val location = com.google.android.gms.tasks.Tasks.await(locationResult)
                lat = location?.latitude
                lon = location?.longitude
            }
        } catch (e: Exception) {
            Log.e("Detection", "Location verification failed", e)
        }

        delay(1500)
        onDetected(bssid, ssid, lat, lon)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Environment Scan",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "We're verifying your location and classroom network connection.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    status,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
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
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var qrDetected by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val cameraState = remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(scanner) {
        onDispose {
            scanner.close()
        }
    }

    fun verifyEverything(qrToken: String) {
        if (isVerifying) return
        isVerifying = true
        scope.launch {
            try {
                // 1. Extract IDs immediately and clean the string
                val rawToken = qrToken.replace("\"", "").replace("'", "").trim()
                
                // Handle different possible separator characters or plain IDs
                val sid = when {
                    rawToken.contains("|") -> rawToken.substringBefore("|")
                    rawToken.contains(":") -> rawToken.substringBefore(":")
                    else -> rawToken
                }
                
                val token = when {
                    rawToken.contains("|") -> rawToken.substringAfter("|")
                    rawToken.contains(":") -> rawToken.substringAfter(":")
                    else -> rawToken
                }
                
                Log.d("Attendance", "QR Scanned! Raw: $qrToken -> SID: $sid")

                // 2. Fire-and-forget the backend checks
                scope.launch {
                    try {
                        RetrofitClient.apiService.verifyWifi(WifiRequest(sid, bssid, ssid, lat, lon))
                        RetrofitClient.apiService.verifyQr(mapOf("session_id" to sid, "token" to token))
                    } catch (e: Exception) {
                        Log.e("Attendance", "Background check failed: ${e.message}")
                    }
                }
                
                // 3. Move to next step IMMEDIATELY
                delay(300) 
                onSuccess(sid)
            } catch (e: Exception) {
                Log.e("Attendance", "Scan error", e)
                qrDetected = false
                isVerifying = false
                onFailure("Failed to process QR code: ${e.message}")
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Scan Faculty QR",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Align the faculty's QR code within the frame.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                            }

                            var currentZoomRatio = 1f
                            val scaleGestureDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                                    currentZoomRatio = cameraState.value?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                                    return true
                                }

                                override fun onScale(detector: ScaleGestureDetector): Boolean {
                                    val cam = cameraState.value ?: return false
                                    val zoomState = cam.cameraInfo.zoomState.value ?: return false
                                    currentZoomRatio *= detector.scaleFactor
                                    currentZoomRatio = currentZoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                                    cam.cameraControl.setZoomRatio(currentZoomRatio)
                                    return true
                                }
                            })

                            previewView.setOnTouchListener { view, event ->
                                scaleGestureDetector.onTouchEvent(event)
                                if (event.action == android.view.MotionEvent.ACTION_UP && event.pointerCount == 1) {
                                    cameraState.value?.let { cam ->
                                        val factory = previewView.meteringPointFactory
                                        val point = factory.createPoint(event.x, event.y)
                                        val action = FocusMeteringAction.Builder(point).build()
                                        cam.cameraControl.startFocusAndMetering(action)
                                    }
                                }
                                view.performClick()
                                true
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build()
                                
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    @AndroidxOptIn(ExperimentalGetImage::class)
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && !qrDetected) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                if (barcodes.isNotEmpty() && !qrDetected) {
                                                    barcodes[0].rawValue?.let { value ->
                                                        qrDetected = true
                                                        verifyEverything(value)
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                    cameraState.value = cam
                                    preview.surfaceProvider = previewView.surfaceProvider
                                } catch (e: Exception) {
                                    Log.e("Camera", "Binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scanner Animation Overlay
                    val infiniteTransition = rememberInfiniteTransition(label = "scan_transition")
                    val scanOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 260f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "scan_animation"
                    )

                    // Pulsing glow effect for the scanning line
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "glow_animation"
                    )

                    if (!isVerifying) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .graphicsLayer {
                                    with(density) {
                                        translationY = -130.dp.toPx() + scanOffset.dp.toPx()
                                    }
                                    alpha = glowAlpha
                                }
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.primary,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Add a corner guide effect
                        val cornerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                                .graphicsLayer { alpha = glowAlpha }
                        ) {
                            val color = cornerColor
                            val s = 3.dp.toPx()
                            val l = 30.dp.toPx()

                            // Top Left
                            drawRect(color, size = Size(l, s))
                            drawRect(color, size = Size(s, l))

                            // Top Right
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(size.width - l, 0f),
                                size = Size(l, s)
                            )
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(size.width - s, 0f),
                                size = Size(s, l)
                            )

                            // Bottom Left
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - s),
                                size = Size(l, s)
                            )
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - l),
                                size = Size(s, l)
                            )

                            // Bottom Right
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    size.width - l,
                                    size.height - s
                                ),
                                size = Size(l, s)
                            )
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    size.width - s,
                                    size.height - l
                                ),
                                size = Size(s, l)
                            )
                        }
                    }

                    if (isVerifying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = FacebookBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaceVerificationStep(
    sessionId: String,
    latitude: Double?,
    longitude: Double?,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("Position your face in the circle") }
    var faceDetected by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableIntStateOf(0) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val cameraState = remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Auto-Brightness Boost for Face Detection Quality
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        val originalBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        
        activity?.window?.let { window ->
            val attrs = window.attributes
            attrs.screenBrightness = 1.0f
            window.attributes = attrs
        }
        
        onDispose {
            activity?.window?.let { window ->
                val attrs = window.attributes
                attrs.screenBrightness = originalBrightness
                window.attributes = attrs
            }
        }
    }

    // Liveness Detection States
    var livenessStep by remember { mutableIntStateOf(0) } // 0: Search, 1: Eyes Open, 2: Blink, 3: Capture
    val livenessTargetBlink = 0.25f // Optimized probability threshold for a closed eye

    // Holographic Typewriter Effect
    var typewriterText by remember { mutableStateOf("") }
    val scanningText = if (livenessStep < 3) "LIVENESS_CHECK_ACTIVE..." else "BIOMETRICS_VERIFIED..."
    val idleText = "AWAITING_FACE_INPUT..."
    val blinkPrompt = "ACTION_REQUIRED: BLINK_EYES"

    LaunchedEffect(faceDetected, livenessStep) {
        val target = when {
            !faceDetected -> idleText
            livenessStep == 0 -> "DETECTION_SUCCESS_WAITING_FOR_EYES..."
            livenessStep == 1 -> "EYES_OPEN_CONFIRMED..."
            livenessStep == 2 -> blinkPrompt
            livenessStep == 3 -> scanningText
            else -> "INITIALIZING_SCANNER..."
        }
        typewriterText = ""
        target.forEach { char ->
            typewriterText += char
            delay(30)
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    // Countdown timer effect
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    DisposableEffect(faceDetector) {
        onDispose {
            faceDetector.close()
        }
    }

    fun takePhoto() {
        if (capturedBitmap != null || isUploading) return
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    val matrix = Matrix()
                    // Mirroring for front camera to match preview. Rotation is handled by toBitmap().
                    matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                    capturedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    statusMessage = "Review your photo"
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("Attendance", "Capture failed", exception)
                    errorMessage = "Capture failed: ${exception.message}"
                }
            }
        )
    }

    fun uploadAndVerify() {
        val bitmap = capturedBitmap ?: return
        isUploading = true
        errorMessage = null
        statusMessage = "Processing Face..."

        val sessionManager = SessionManager(context)
        val studentIdRaw = sessionManager.getUserId() ?: "UNKNOWN_STUDENT"
        val studentId = studentIdRaw.replace("\"", "").replace("'", "").trim()
        val cleanSessionId = sessionId.replace("\"", "").replace("'", "").trim()

        scope.launch {
            try {
                val faceDir = File(context.cacheDir, "face")
                if (!faceDir.exists()) faceDir.mkdirs()
                
                val file = File(faceDir, "face_${studentId}_${System.currentTimeMillis()}.jpg")
                val out = FileOutputStream(file)

                // --- OPTIMIZATION: Resize and Compress for Cloud Speed ---
                val scaledBitmap = if (bitmap.width > 800 || bitmap.height > 800) {
                    val scaleFactor = 800f / maxOf(bitmap.width, bitmap.height)
                    bitmap.scale(
                        (bitmap.width * scaleFactor).toInt(),
                        (bitmap.height * scaleFactor).toInt()
                    )
                } else {
                    bitmap
                }
                
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out) // Lowered quality to 70 for speed
                out.flush()
                out.close()
                // ---------------------------------------------------------

                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                
                val studentPart = studentId.toRequestBody("text/plain".toMediaTypeOrNull())
                val sessionPart = cleanSessionId.toRequestBody("text/plain".toMediaTypeOrNull())
                val latPart = latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val lonPart = longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                statusMessage = "Uploading to Server..."

                val res = RetrofitClient.apiService.verifyFace(
                    image = imagePart,
                    studentId = studentPart,
                    sessionId = sessionPart,
                    latitude = latPart,
                    longitude = lonPart
                )

                if (res.isSuccessful) {
                    statusMessage = "Attendance Verified!"
                    delay(1000)
                    onSuccess()
                } else {
                    val error = res.errorBody()?.string() ?: "Unknown error"
                    Log.e("Attendance", "Verification failed: $error")

                    if (error.contains("Network") || error.contains("Session")) {
                        onFailure(error)
                        return@launch
                    }
                    
                    val displayError = if (error.contains("detail")) {
                         val detail = error.substringAfter("\"detail\":\"").substringBefore("\"")
                         if (detail.contains("Face could not be detected")) {
                             "Face not detected. Please ensure your face is clearly visible and within the frame."
                         } else if (detail.contains("match")) {
                             "Face mismatch! Please ensure you are the registered student."
                         } else {
                             detail
                         }
                    } else {
                        "Face does not match our records"
                    }
                    
                    errorMessage = displayError
                    statusMessage = "Verification Failed"
                    isUploading = false
                    countdown = 5
                }
            } catch (e: Exception) {
                Log.e("Attendance", "Face processing error: ${e.message}", e)
                val isStreamEnd = e.message?.contains("unexpected end of stream") == true ||
                                 e.message?.contains("Software caused connection abort") == true

                errorMessage = if (isStreamEnd) {
                    "Server connection reset. Retrying upload might help."
                } else {
                    "Network error. Check connection."
                }
                statusMessage = "Error occurred"
                isUploading = false
                countdown = 3
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Face Verification",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Help Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = YellowAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Hold phone at eye level in a well-lit area.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .border(
                            4.dp,
                            when {
                                isUploading -> MaterialTheme.colorScheme.primary
                                errorMessage != null -> RedAccent
                                capturedBitmap != null -> FacebookBlue
                                faceDetected -> FacebookBlue
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Quality Meter Overlay
                    if (capturedBitmap == null && faceDetected) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "FACE_INTEGRITY",
                                color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(5) { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 12.dp, height = 4.dp)
                                            .background(
                                                if (livenessStep > i) Color.Green else Color(0xFF00E5FF).copy(alpha = 0.2f),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    if (capturedBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Face",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                                }

                                var currentZoomRatio = 1f
                                val scaleGestureDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                                        currentZoomRatio = cameraState.value?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                                        return true
                                    }

                                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                                        val cam = cameraState.value ?: return false
                                        val zoomState = cam.cameraInfo.zoomState.value ?: return false
                                        currentZoomRatio *= detector.scaleFactor
                                        currentZoomRatio = currentZoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                                        cam.cameraControl.setZoomRatio(currentZoomRatio)
                                        return true
                                    }
                                })

                                previewView.setOnTouchListener { view, event ->
                                    scaleGestureDetector.onTouchEvent(event)
                                    if (event.action == android.view.MotionEvent.ACTION_UP && event.pointerCount == 1) {
                                        cameraState.value?.let { cam ->
                                            val factory = previewView.meteringPointFactory
                                            val point = factory.createPoint(event.x, event.y)
                                            val action = FocusMeteringAction.Builder(point).build()
                                            cam.cameraControl.startFocusAndMetering(action)
                                        }
                                    }
                                    view.performClick()
                                    true
                                }

                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build()
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                        @AndroidxOptIn(ExperimentalGetImage::class)
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null && capturedBitmap == null && countdown == 0) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            faceDetector.process(image)
                                                .addOnSuccessListener { faces ->
                                                    if (faces.isNotEmpty()) {
                                                        val face = faces[0]
                                                        faceDetected = true
                                                        
                                                        val leftOpen = face.leftEyeOpenProbability ?: 1.0f
                                                        val rightOpen = face.rightEyeOpenProbability ?: 1.0f

                                                        // Liveness Logic - 3-Phase Verification
                                                        when (livenessStep) {
                                                            0 -> { // Phase 1: Ensure eyes are open initially
                                                                if (leftOpen > 0.7f && rightOpen > 0.7f) {
                                                                    livenessStep = 1
                                                                    statusMessage = "Blink your eyes"
                                                                } else {
                                                                    statusMessage = "Open your eyes wide"
                                                                }
                                                            }
                                                            1 -> { // Phase 2: Detect Blink
                                                                if (leftOpen < livenessTargetBlink || rightOpen < livenessTargetBlink) {
                                                                    livenessStep = 2
                                                                    statusMessage = "Blink detected! Keep still"
                                                                }
                                                            }
                                                            2 -> { // Phase 3: Final capture when eyes open again
                                                                if (leftOpen > 0.6f && rightOpen > 0.6f) {
                                                                    livenessStep = 3
                                                                    statusMessage = "Capturing..."
                                                                    scope.launch {
                                                                        delay(300)
                                                                        takePhoto()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        faceDetected = false
                                                        livenessStep = 0
                                                        if (errorMessage == null && countdown == 0) statusMessage =
                                                            "Looking for face..."
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                    try {
                                        cameraProvider.unbindAll()
                                        val cam = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis,
                                            imageCapture
                                        )
                                        cameraState.value = cam
                                        preview.surfaceProvider = previewView.surfaceProvider
                                    } catch (e: Exception) {
                                        Log.e("Camera", "Use case binding failed", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // --- HOLOGRAPHIC SCANNING OVERLAY ---
                    if (capturedBitmap == null && !isUploading) {
                        // --- FACE QUALITY & INTEGRITY HUD ---
                        if (faceDetected) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 15.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "INTEGRITY_SHIELD_ACTIVE",
                                    color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(5) { i ->
                                        Box(
                                            modifier = Modifier
                                                .size(width = 14.dp, height = 3.dp)
                                                .background(
                                                    if (faceDetected && i <= livenessStep + 1) 
                                                        Color(0xFF00E5FF) 
                                                    else 
                                                        Color.White.copy(alpha = 0.2f),
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        // -------------------------------------

                        val infiniteTransition = rememberInfiniteTransition(label = "hologram")
                        val laserPos by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 220f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ), label = "laser"
                        )
                        
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 0.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "glow"
                        )

                        // Moving Laser Grid Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .graphicsLayer {
                                    with(density) {
                                        translationY = -110.dp.toPx() + laserPos.dp.toPx()
                                    }
                                    alpha = if (faceDetected) 0.9f else 0.4f
                                }
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            (if (livenessStep == 3) Color.Green else Color(0xFF00E5FF)),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Cybernetic Corner Brackets
                        Canvas(modifier = Modifier
                            .fillMaxSize()
                            .padding(35.dp)
                            .graphicsLayer { alpha = glowAlpha }
                        ) {
                            val color = if (livenessStep == 3) Color.Green else Color(0xFF00E5FF)
                            val s = 2.dp.toPx()
                            val l = 25.dp.toPx()

                            // Top Left
                            drawRect(color, size = Size(l, s))
                            drawRect(color, size = Size(s, l))

                            // Top Right
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(size.width - l, 0f),
                                size = Size(l, s)
                            )
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(size.width - s, 0f),
                                size = Size(s, l)
                            )

                            // Bottom Left
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - s),
                                size = Size(l, s)
                            )
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - l),
                                size = Size(s, l)
                            )

                            // Bottom Right
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    size.width - l,
                                    size.height - s
                                ),
                                size = Size(l, s)
                            )
                            drawRect(
                                color,
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    size.width - s,
                                    size.height - l
                                ),
                                size = Size(s, l)
                            )
                        }

                        // Terminal Style Text Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 30.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "> $typewriterText",
                                color = Color(0xFF00E5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    if (isUploading) {
                        CircularProgressIndicator(color = FacebookBlue, modifier = Modifier.size(60.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    statusMessage,
                    color = when {
                        errorMessage != null -> RedAccent
                        capturedBitmap != null -> FacebookBlue
                        faceDetected -> FacebookBlue
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (capturedBitmap != null && !isUploading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                capturedBitmap = null
                                faceDetected = false
                                errorMessage = null
                                statusMessage = "Position your face in the circle"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Retake",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { uploadAndVerify() },
                            colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload", fontSize = 14.sp, color = Color.White)
                        }
                    }
                } else if (capturedBitmap == null && !isUploading && countdown == 0) {
                    Button(
                        onClick = { takePhoto() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (faceDetected) FacebookBlue else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (faceDetected) "Capture Photo" else "Manual Capture",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        errorMessage!!,
                        color = RedAccent.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (countdown > 0) {
                        Text(
                            "Please wait ${countdown}s...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Button(
                            onClick = {
                                errorMessage = null
                                faceDetected = false
                                capturedBitmap = null
                                countdown = 0
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Try Again Now",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessStep(sessionId: String, onFinish: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userName = sessionManager.getName()?.replace("\"", "") ?: "Student"
    var visible by remember { mutableStateOf(false) }
    var sessionDetails by remember { mutableStateOf<SessionDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        visible = true
        try {
            val response = RetrofitClient.apiService.getSessionDetails(sessionId)
            if (response.isSuccessful) {
                sessionDetails = response.body()
            }
        } catch (e: Exception) {
            Log.e("Attendance", "Failed to load session details", e)
        } finally {
            isLoading = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(1000)) + slideInVertically(tween(1000)) { it / 2 }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FacebookBlue.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(FacebookBlue.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Verified,
                        null,
                        tint = FacebookBlue,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Verification Successful",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Authenticated as $userName",
                    color = FacebookBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Institutional Receipt Look
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "ATTENDANCE_RECEIPT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = FacebookBlue,
                                trackColor = FacebookBlue.copy(alpha = 0.1f)
                            )
                        } else {
                            ReceiptRow("SUBJECT", sessionDetails?.subject_name ?: "Processing...")
                            ReceiptRow("SESSION_ID", "#${sessionId.take(8).uppercase()}")
                            ReceiptRow("TIMESTAMP", DateTimeUtils.formatTimeOnly(sessionDetails?.start_time ?: isoFormat.format(Date())))
                            ReceiptRow("STATUS", "CONFIRMED", color = Color(0xFF4CAF50))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        "DONE",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            textAlign = TextAlign.End
        )
    }
}

private val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)

@Composable
fun PermissionSection(onRequest: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Permissions Required", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "AttendX needs Camera and Location access to verify your presence in the classroom.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Grant Access", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val step = index + 1
            val isActive = step <= currentStep
            val isCompleted = step < currentStep
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            CircleShape
                        )
                        .border(
                            2.dp,
                            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            step.toString(),
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (index < 2) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .background(if (step < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
            }
        }
    }
}
