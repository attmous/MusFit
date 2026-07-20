package com.musfit.ui.food

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.musfit.feature.food.R
import com.musfit.ui.components.PillButton
import com.musfit.ui.theme.CameraSurface
import com.musfit.ui.theme.Cream
import com.musfit.ui.theme.MusFitTheme

internal enum class CameraPermissionAction { RequestPermission, OpenAppSettings }

internal data class CameraPermissionAccess(
    val isGranted: Boolean,
    val action: CameraPermissionAction,
    val performAction: () -> Unit,
)

@Composable
internal fun rememberCameraPermissionAccess(): CameraPermissionAccess {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var initialRequestStarted by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isGranted = granted
        permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }

    LaunchedEffect(isGranted, initialRequestStarted) {
        if (!isGranted && !initialRequestStarted) {
            initialRequestStarted = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (isGranted) permanentlyDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val action = cameraPermissionAction(permanentlyDenied)
    return CameraPermissionAccess(
        isGranted = isGranted,
        action = action,
        performAction = {
            when (action) {
                CameraPermissionAction.RequestPermission -> launcher.launch(Manifest.permission.CAMERA)
                CameraPermissionAction.OpenAppSettings -> context.openAppSettings()
            }
        },
    )
}

internal fun cameraPermissionAction(permanentlyDenied: Boolean): CameraPermissionAction = if (permanentlyDenied) CameraPermissionAction.OpenAppSettings else CameraPermissionAction.RequestPermission

@Composable
internal fun CameraPermissionDeniedContent(
    message: String,
    action: CameraPermissionAction,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraSurface)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = message, style = MusFitTheme.typography.bodyLarge, color = Cream)
        PillButton(
            text = stringResource(
                if (action == CameraPermissionAction.OpenAppSettings) {
                    R.string.food_open_app_settings
                } else {
                    R.string.food_grant_camera_access
                },
            ),
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
