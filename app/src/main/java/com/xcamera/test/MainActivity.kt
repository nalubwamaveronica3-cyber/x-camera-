package com.xcamera.test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("X Camera", "App started - X Camera test app")

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }

        // Capture button click listener
        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // ImageCapture use case (THIS IS WHAT WE'RE TESTING)
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind any previous use cases and bind new ones
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                Log.d("X Camera", "Camera started successfully")
            } catch (e: Exception) {
                Log.e("X Camera", "Camera failed to start: ${e.message}")
                Toast.makeText(this, "Camera failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        Log.d("X Camera", "========================================")
        Log.d("X Camera", "takePhoto() called - capture button pressed!")

        // Create a unique file name for the captured image
        val photoFile = java.io.File(
            externalMediaDirs.firstOrNull(),
            "test_photo_${System.currentTimeMillis()}.jpg"
        )

        Log.d("X Camera", "Photo file path: ${photoFile.absolutePath}")

        // Check if imageCapture is initialized
        if (!::imageCapture.isInitialized) {
            Log.e("X Camera", "❌ imageCapture is not initialized!")
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        Log.d("X Camera", "Calling imageCapture.takePicture()...")

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("X Camera", "✅ onImageSaved CALLED!")
                    Log.d("X Camera", "Photo saved: ${photoFile.absolutePath}")
                    Log.d("X Camera", "========================================")
                    Toast.makeText(
                        this@MainActivity,
                        "Photo saved: ${photoFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("X Camera", "❌ onError CALLED!")
                    Log.e("X Camera", "Capture failed: ${exception.message}")
                    Log.e("X Camera", "========================================")
                    Toast.makeText(
                        this@MainActivity,
                        "Capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        Log.d("X Camera", "takePicture() call complete - waiting for callback...")
        Log.d("X Camera", "========================================")
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Log.e("X Camera", "Camera permission denied")
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
