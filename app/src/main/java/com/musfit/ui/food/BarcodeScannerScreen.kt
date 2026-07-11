package com.musfit.ui.food

import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.OptIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.musfit.ui.components.PillButton
import com.musfit.ui.theme.CameraSurface
import com.musfit.ui.theme.CameraTranslucent
import com.musfit.ui.theme.Cream
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.ViewfinderBracket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detectionHandled = remember { AtomicBoolean(false) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted },
    )
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                )
                .build(),
        )
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(scanner, analysisExecutor) {
        onDispose {
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        if (hasCameraPermission) {
            val executor = ContextCompat.getMainExecutor(context)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { cameraPreview ->
                        cameraPreview.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        if (detectionHandled.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val barcode = barcodes.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotBlank) }
                                if (barcode != null && detectionHandled.compareAndSet(false, true)) {
                                    analysis.clearAnalyzer()
                                    cameraProvider.unbindAll()
                                    onBarcodeDetected(barcode)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }

                    cameraProvider.unbindAll()
                    boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                },
                executor,
            )
        }

        onDispose {
            boundCamera = null
            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CameraSurface)
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Camera permission is required to scan barcodes.",
                style = MusFitTheme.typography.bodyLarge,
                color = Cream,
            )
            PillButton(
                text = "Grant camera access",
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraSurface),
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CameraCircleButton(
                onClick = onClose,
                contentDescription = "Close scanner",
                icon = Icons.Outlined.Close,
            )
            Text(
                text = "Scan barcode",
                style = MusFitTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Cream,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            val camera = boundCamera
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                CameraCircleButton(
                    onClick = {
                        torchEnabled = !torchEnabled
                        camera.cameraControl.enableTorch(torchEnabled)
                    },
                    contentDescription = if (torchEnabled) "Turn flashlight off" else "Turn flashlight on",
                    icon = if (torchEnabled) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                )
            } else {
                // Keeps the title optically centered when no torch is available.
                Box(modifier = Modifier.size(44.dp))
            }
        }
        ViewfinderBrackets(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 250.dp, height = 160.dp),
        )
        Surface(
            onClick = onClose,
            shape = CircleShape,
            color = CameraTranslucent,
            contentColor = Cream,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Or enter the code manually",
                style = MusFitTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CameraCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = CameraTranslucent,
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Cream,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ViewfinderBrackets(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        val arm = 34.dp.toPx()
        val radius = 14.dp.toPx()
        val w = size.width
        val h = size.height

        fun bracket(builder: Path.() -> Unit) = drawPath(Path().apply(builder), ViewfinderBracket, style = stroke)

        // Top-left
        bracket {
            moveTo(0f, arm)
            lineTo(0f, radius)
            arcTo(androidx.compose.ui.geometry.Rect(Offset.Zero, Size(radius * 2, radius * 2)), 180f, 90f, false)
            lineTo(arm, 0f)
        }
        // Top-right
        bracket {
            moveTo(w - arm, 0f)
            lineTo(w - radius, 0f)
            arcTo(androidx.compose.ui.geometry.Rect(Offset(w - radius * 2, 0f), Size(radius * 2, radius * 2)), 270f, 90f, false)
            lineTo(w, arm)
        }
        // Bottom-right
        bracket {
            moveTo(w, h - arm)
            lineTo(w, h - radius)
            arcTo(androidx.compose.ui.geometry.Rect(Offset(w - radius * 2, h - radius * 2), Size(radius * 2, radius * 2)), 0f, 90f, false)
            lineTo(w - arm, h)
        }
        // Bottom-left
        bracket {
            moveTo(arm, h)
            lineTo(radius, h)
            arcTo(androidx.compose.ui.geometry.Rect(Offset(0f, h - radius * 2), Size(radius * 2, radius * 2)), 90f, 90f, false)
            lineTo(0f, h - arm)
        }
    }
}
