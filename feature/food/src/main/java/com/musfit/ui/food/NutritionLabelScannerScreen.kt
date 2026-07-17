package com.musfit.ui.food

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.musfit.integrations.scanner.NutritionLabelScanResult
import com.musfit.integrations.scanner.NutritionLabelScannerController

@Composable
fun NutritionLabelScannerScreen(onLabelCaptured: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberCameraPermissionAccess()
    val controller = remember(cameraPermission.isGranted, lifecycleOwner) { NutritionLabelScannerController(context) }
    var latestResult by remember { mutableStateOf<NutritionLabelScanResult?>(null) }

    DisposableEffect(controller, lifecycleOwner, cameraPermission.isGranted) {
        if (cameraPermission.isGranted) {
            controller.start(
                lifecycleOwner = lifecycleOwner,
                onPreviewResult = { latestResult = it },
                onFailure = { latestResult = null },
            )
        }
        onDispose { controller.close() }
    }

    if (!cameraPermission.isGranted) {
        CameraPermissionDeniedContent(
            message = "Camera permission is required to scan nutrition labels.",
            action = cameraPermission.action,
            onAction = cameraPermission.performAction,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { controller.previewView }, modifier = Modifier.fillMaxSize())
        Text(
            text = "Point at the nutrition label, then capture",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        Button(
            onClick = {
                latestResult?.let { result ->
                    controller.capture(result) { onLabelCaptured(it.text) }
                }
            },
            enabled = latestResult != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        ) {
            Text(if (latestResult == null) "Reading..." else "Capture label")
        }
    }
}
