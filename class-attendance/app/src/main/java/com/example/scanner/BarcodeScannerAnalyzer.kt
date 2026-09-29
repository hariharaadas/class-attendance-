package com.example.scanner

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX ImageAnalysis analyzer for continuous real-time 1D & 2D barcode detection.
 * Optimized for standard 1D student ID card barcodes (Code 128, Code 39, EAN, UPC, etc.).
 */
class BarcodeScannerAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Configure scanner for standard 1D barcodes and QR codes
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    private var lastScannedValue: String? = null
    private var lastScannedTime: Long = 0L
    private val duplicateDebounceMs: Long = 1300L // Prevent continuous spamming of the exact same physical card

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue?.trim()
                    if (!rawValue.isNullOrEmpty()) {
                        val now = SystemClock.elapsedRealtime()
                        // Allow immediate scan if a new student ID appears,
                        // or if the same card has been presented after the debounce window
                        if (rawValue != lastScannedValue || (now - lastScannedTime) > duplicateDebounceMs) {
                            lastScannedValue = rawValue
                            lastScannedTime = now
                            onBarcodeDetected(rawValue)
                            break // Process one primary barcode per frame
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Ignore transient frame analysis errors and keep analyzing next frames
            }
            .addOnCompleteListener {
                // Must close imageProxy to allow CameraX to deliver the next frame
                imageProxy.close()
            }
    }
}
