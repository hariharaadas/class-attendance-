package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scanner.BarcodeScannerAnalyzer
import com.example.scanner.ScannerFeedbackHelper
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.ScanResult
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onDoneScanning: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val feedbackHelper = remember { ScannerFeedbackHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            feedbackHelper.release()
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var scanFlashColor by remember { mutableStateOf<Color?>(null) }
    var showManualAddDialog by remember { mutableStateOf(false) }

    // Visual pulse effect on scan
    LaunchedEffect(activeSession.lastScanResult) {
        when (val result = activeSession.lastScanResult) {
            is ScanResult.Success -> {
                feedbackHelper.playSuccessFeedback()
                scanFlashColor = Color(0xFF10B981) // Green flash
                delay(400)
                scanFlashColor = null
            }
            is ScanResult.Duplicate -> {
                feedbackHelper.playDuplicateFeedback()
                scanFlashColor = Color(0xFFF59E0B) // Amber flash
                delay(400)
                scanFlashColor = null
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("scanner_screen")
    ) {
        if (hasCameraPermission) {
            // CameraX Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(
                                    cameraExecutor,
                                    BarcodeScannerAnalyzer { scannedRollNumber ->
                                        viewModel.processRollNumber(scannedRollNumber)
                                    }
                                )
                            }

                        try {
                            cameraProvider.unbindAll()
                            val boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            camera = boundCamera
                        } catch (_: Exception) {
                            // Camera binding exception
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning Overlay: Darkened edges with transparent 1D barcode guide window & corner brackets
            val bracketColor = scanFlashColor ?: Color.White
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // 1D Barcode scanning window: wide horizontal rectangle
                val scanBoxWidth = (canvasWidth * 0.82f).coerceAtMost(360.dp.toPx())
                val scanBoxHeight = (canvasHeight * 0.24f).coerceAtMost(180.dp.toPx())
                val left = (canvasWidth - scanBoxWidth) / 2f
                val top = (canvasHeight - scanBoxHeight) / 2f - 40.dp.toPx()
                val right = left + scanBoxWidth
                val bottom = top + scanBoxHeight

                val cutoutRect = Rect(left, top, right, bottom)
                val cutoutPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = cutoutRect,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    )
                }

                // Draw darkened scrim outside the scanning window
                clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                    drawRect(color = Color(0x99000000))
                }

                // Draw four corner guide brackets (No laser line)
                val cornerLength = 28.dp.toPx()
                val strokeW = 4.dp.toPx()

                // Top-Left
                drawLine(
                    color = bracketColor,
                    start = Offset(left, top + cornerLength),
                    end = Offset(left, top + 8.dp.toPx()),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(left + 8.dp.toPx(), top),
                    end = Offset(left + cornerLength, top),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )

                // Top-Right
                drawLine(
                    color = bracketColor,
                    start = Offset(right - cornerLength, top),
                    end = Offset(right - 8.dp.toPx(), top),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(right, top + 8.dp.toPx()),
                    end = Offset(right, top + cornerLength),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )

                // Bottom-Left
                drawLine(
                    color = bracketColor,
                    start = Offset(left, bottom - cornerLength),
                    end = Offset(left, bottom - 8.dp.toPx()),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(left + 8.dp.toPx(), bottom),
                    end = Offset(left + cornerLength, bottom),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )

                // Bottom-Right
                drawLine(
                    color = bracketColor,
                    start = Offset(right - cornerLength, bottom),
                    end = Offset(right - 8.dp.toPx(), bottom),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(right, bottom - 8.dp.toPx()),
                    end = Offset(right, bottom - cornerLength),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // Top HUD Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .testTag("scanner_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Present Counter Pill
                Surface(
                    color = Color(0xCC0F172A),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("scanner_present_counter")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Present: ${activeSession.presentRollNumbers.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Torch Toggle Button
                IconButton(
                    onClick = {
                        val newTorchState = !isTorchEnabled
                        isTorchEnabled = newTorchState
                        camera?.cameraControl?.enableTorch(newTorchState)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isTorchEnabled) Color(0xFFFFD54F) else Color(0x66000000))
                        .testTag("scanner_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isTorchEnabled) Color.Black else Color.White
                    )
                }
            }

            // Central Guide Label directly above viewfinder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 180.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xAA000000),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Point camera at student ID barcode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Bottom Controller & Feedback Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Real-time scan result notification card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (activeSession.lastScanResult) {
                            is ScanResult.Success -> Color(0xFF064E3B)
                            is ScanResult.Duplicate -> Color(0xFF78350F)
                            else -> Color(0xDD1E293B)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            when (val result = activeSession.lastScanResult) {
                                is ScanResult.Success -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "STUDENT MARKED PRESENT",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = result.rollNumber,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                                is ScanResult.Duplicate -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ALREADY MARKED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = result.rollNumber,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "${activeSession.classSection} • ${activeSession.hour}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (activeSession.lastScannedRollNumber != null)
                                            "Recently: ${activeSession.lastScannedRollNumber}"
                                        else "Scanning continuously...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Manual Add Button in Corner
                        IconButton(
                            onClick = { showManualAddDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .testTag("scanner_manual_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Manual Add",
                                tint = Color.White
                            )
                        }
                    }
                }

                // DONE Button (Stops camera and proceeds to Review screen)
                Button(
                    onClick = onDoneScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_scanner_done"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DONE (${activeSession.presentRollNumbers.size} STUDENTS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        } else {
            // Permission Denied / Request View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Class Attendance needs camera access to scan student ID barcodes in real-time. Images are never recorded or saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Camera Permission")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Go Back")
                }
            }
        }
    }

    if (showManualAddDialog) {
        ManualAddDialog(
            onDismiss = { showManualAddDialog = false },
            onAddRollNumber = { roll -> viewModel.processRollNumber(roll) }
        )
    }
}
