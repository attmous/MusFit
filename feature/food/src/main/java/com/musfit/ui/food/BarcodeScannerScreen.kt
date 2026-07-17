package com.musfit.ui.food

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.musfit.integrations.scanner.BarcodeScannerController
import com.musfit.ui.theme.CameraSurface
import com.musfit.ui.theme.CameraTranslucent
import com.musfit.ui.theme.Cream
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.ViewfinderBracket

@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current
    val cameraPermission = rememberCameraPermissionAccess()

    if (!cameraPermission.isGranted) {
        CameraPermissionDeniedContent(
            message = "Camera permission is required to scan barcodes.",
            action = cameraPermission.action,
            onAction = cameraPermission.performAction,
        )
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(lifecycleOwner) { BarcodeScannerController(context) }
    var cameraReady by remember { mutableStateOf(false) }

    DisposableEffect(controller, lifecycleOwner) {
        controller.start(
            lifecycleOwner = lifecycleOwner,
            onResult = { onBarcodeDetected(it.value) },
            onCameraReady = { cameraReady = true },
            onFailure = { cameraReady = false },
        )
        onDispose {
            cameraReady = false
            controller.close()
        }
    }

    BarcodeScannerCameraContent(
        controller = controller,
        cameraReady = cameraReady,
        onClose = onClose,
    )
}

@Composable
private fun BarcodeScannerCameraContent(
    controller: BarcodeScannerController,
    cameraReady: Boolean,
    onClose: () -> Unit,
) {
    var torchEnabled by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().background(CameraSurface)) {
        AndroidView(factory = { controller.previewView }, modifier = Modifier.fillMaxSize())
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CameraCircleButton(onClose, "Close scanner", Icons.Outlined.Close)
            Text(
                text = "Scan barcode",
                style = MusFitTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Cream,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (cameraReady && controller.hasFlashUnit) {
                CameraCircleButton(
                    onClick = {
                        torchEnabled = !torchEnabled
                        controller.setTorchEnabled(torchEnabled)
                    },
                    contentDescription = if (torchEnabled) "Turn flashlight off" else "Turn flashlight on",
                    icon = if (torchEnabled) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                )
            } else {
                Box(modifier = Modifier.size(44.dp))
            }
        }
        ViewfinderBrackets(
            modifier = Modifier.align(Alignment.Center).size(width = 250.dp, height = 160.dp),
        )
        Surface(
            onClick = onClose,
            shape = CircleShape,
            color = CameraTranslucent,
            contentColor = Cream,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
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
internal fun BarcodeScannerPermissionDeniedContent(onGrantCameraAccess: () -> Unit) {
    CameraPermissionDeniedContent(
        message = "Camera permission is required to scan barcodes.",
        action = CameraPermissionAction.RequestPermission,
        onAction = onGrantCameraAccess,
    )
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

        bracket {
            moveTo(0f, arm)
            lineTo(0f, radius)
            arcTo(androidx.compose.ui.geometry.Rect(Offset.Zero, Size(radius * 2, radius * 2)), 180f, 90f, false)
            lineTo(arm, 0f)
        }
        bracket {
            moveTo(w - arm, 0f)
            lineTo(w - radius, 0f)
            arcTo(androidx.compose.ui.geometry.Rect(Offset(w - radius * 2, 0f), Size(radius * 2, radius * 2)), 270f, 90f, false)
            lineTo(w, arm)
        }
        bracket {
            moveTo(w, h - arm)
            lineTo(w, h - radius)
            arcTo(androidx.compose.ui.geometry.Rect(Offset(w - radius * 2, h - radius * 2), Size(radius * 2, radius * 2)), 0f, 90f, false)
            lineTo(w - arm, h)
        }
        bracket {
            moveTo(arm, h)
            lineTo(radius, h)
            arcTo(androidx.compose.ui.geometry.Rect(Offset(0f, h - radius * 2), Size(radius * 2, radius * 2)), 90f, 90f, false)
            lineTo(0f, h - arm)
        }
    }
}
