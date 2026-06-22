package com.musfit.ui.food

import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.OptIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalGetImage::class)
@Composable
fun NutritionLabelScannerScreen(
    onLabelCaptured: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val captureHandled = remember { AtomicBoolean(false) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var latestText by remember { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted },
    )
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(recognizer, analysisExecutor) {
        onDispose {
            recognizer.close()
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
                        if (captureHandled.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        recognizer.process(image)
                            .addOnSuccessListener { result ->
                                if (!captureHandled.get()) {
                                    latestText = result.text
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
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
            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Camera permission is required to scan nutrition labels.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(text = "Grant camera access")
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = "Point at the nutrition label, then capture",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        Button(
            onClick = {
                if (latestText.isNotBlank() && captureHandled.compareAndSet(false, true)) {
                    if (cameraProviderFuture.isDone) {
                        cameraProviderFuture.get().unbindAll()
                    }
                    onLabelCaptured(latestText)
                }
            },
            enabled = latestText.isNotBlank(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
        ) {
            Text(if (latestText.isBlank()) "Reading…" else "Capture label")
        }
    }
}
