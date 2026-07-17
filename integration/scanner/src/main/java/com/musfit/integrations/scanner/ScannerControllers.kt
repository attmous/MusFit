package com.musfit.integrations.scanner

import android.content.Context
import android.view.View
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@JvmInline
value class BarcodeScanResult(val value: String)

@JvmInline
value class NutritionLabelScanResult(val text: String)

class BarcodeScannerController(context: Context) : AutoCloseable {
    private val preview = PreviewView(context)
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val detectionHandled = AtomicBoolean(false)
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build(),
    )
    private var session: LifecycleSafeCameraSession<ProcessCameraProvider, UseCase, Camera>? = null
    private var ownedAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null

    val previewView: View get() = preview
    val hasFlashUnit: Boolean get() = camera?.cameraInfo?.hasFlashUnit() == true

    @OptIn(ExperimentalGetImage::class)
    fun start(
        lifecycleOwner: LifecycleOwner,
        onResult: (BarcodeScanResult) -> Unit,
        onCameraReady: () -> Unit,
        onFailure: (Throwable) -> Unit = {},
    ) {
        check(session == null) { "Barcode scanner already started." }
        val cameraPreview = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        ownedAnalysis = analysis
        val owner = LifecycleSafeCameraSession<ProcessCameraProvider, UseCase, Camera>(
            providerFuture = ProcessCameraProvider.getInstance(preview.context),
            callbackExecutor = ContextCompat.getMainExecutor(preview.context),
            bindUseCases = { provider, useCases ->
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCases[0],
                    useCases[1],
                )
            },
            unbindUseCases = { provider, useCases -> provider.unbind(useCases[0], useCases[1]) },
            onFailure = onFailure,
        )
        session = owner
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
            scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { barcodes ->
                    val value = barcodes.firstNotNullOfOrNull {
                        it.rawValue?.trim()?.takeIf(String::isNotBlank)
                    }
                    if (value != null && detectionHandled.compareAndSet(false, true)) {
                        analysis.clearAnalyzer()
                        owner.unbindOwned()
                        onResult(BarcodeScanResult(value))
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        }
        owner.start(listOf(cameraPreview, analysis)) {
            camera = it
            onCameraReady()
        }
    }

    fun setTorchEnabled(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    override fun close() {
        camera = null
        ownedAnalysis?.clearAnalyzer()
        ownedAnalysis = null
        session?.close()
        session = null
        scanner.close()
        analysisExecutor.shutdown()
    }
}

class NutritionLabelScannerController(context: Context) : AutoCloseable {
    private val preview = PreviewView(context)
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val captureHandled = AtomicBoolean(false)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var session: LifecycleSafeCameraSession<ProcessCameraProvider, UseCase, Camera>? = null

    private var ownedAnalysis: ImageAnalysis? = null
    val previewView: View get() = preview

    @OptIn(ExperimentalGetImage::class)
    fun start(
        lifecycleOwner: LifecycleOwner,
        onPreviewResult: (NutritionLabelScanResult) -> Unit,
        onFailure: (Throwable) -> Unit = {},
    ) {
        check(session == null) { "Nutrition label scanner already started." }
        val cameraPreview = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        ownedAnalysis = analysis
        val owner = LifecycleSafeCameraSession<ProcessCameraProvider, UseCase, Camera>(
            providerFuture = ProcessCameraProvider.getInstance(preview.context),
            callbackExecutor = ContextCompat.getMainExecutor(preview.context),
            bindUseCases = { provider, useCases ->
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCases[0],
                    useCases[1],
                )
            },
            unbindUseCases = { provider, useCases -> provider.unbind(useCases[0], useCases[1]) },
            onFailure = onFailure,
        )
        session = owner
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
            recognizer.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { result ->
                    result.text.takeIf(String::isNotBlank)?.let {
                        onPreviewResult(NutritionLabelScanResult(it))
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        }
        owner.start(listOf(cameraPreview, analysis)) {}
    }

    fun capture(result: NutritionLabelScanResult, onCaptured: (NutritionLabelScanResult) -> Unit) {
        if (captureHandled.compareAndSet(false, true)) {
            session?.unbindOwned()
            onCaptured(result)
        }
    }

    override fun close() {
        session?.close()
        session = null
        ownedAnalysis?.clearAnalyzer()
        ownedAnalysis = null
        recognizer.close()
        analysisExecutor.shutdown()
    }
}
