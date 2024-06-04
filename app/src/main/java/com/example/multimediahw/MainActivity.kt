package com.example.multimediahw

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.multimediahw.detector.GraphicOverlay
import com.example.multimediahw.detector.face.FaceDetectorProcessor
import com.example.multimediahw.detector.`object`.ObjectDetectorProcessor
import com.example.multimediahw.ui.screens.camera.CameraView
import com.example.multimediahw.ui.screens.camera.CameraViewModel
import com.example.multimediahw.ui.theme.MultimediaHWTheme
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class MainActivity : ComponentActivity() {

    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var previewUseCase: androidx.camera.core.Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var faceDetectionProcessor: FaceDetectorProcessor? = null
    private var objectDetectionProcessor: ObjectDetectorProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraSelector: CameraSelector? = null

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = PermissionManager(this)
        pm.requestCameraPermission()

        graphicOverlay = GraphicOverlay(this, null)

        faceDetectionProcessor = FaceDetectorProcessor(this,
            FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .enableTracking()
            .build())
        Log.e(TAG, "faceDetectionProcessor ${faceDetectionProcessor != null}")

        /*
                objectDetectionProcessor = ObjectDetectorProcessor(this,
                    CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.5f)
                        .setMaxPerObjectLabelCount(3)
                        .build())*/
        analysisUseCase = ImageAnalysis.Builder().build()

        needUpdateGraphicOverlayImageSourceInfo = true
        Log.e(TAG, "2")
        Log.e(TAG, "analysisUseCase? ${analysisUseCase != null}")

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this),
            ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                if (needUpdateGraphicOverlayImageSourceInfo) {
                    Log.e(TAG, "needUpdateGraphicOverlayImageSourceInfo")

                    val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.width,
                            imageProxy.height,
                            isImageFlipped
                        )
                    } else {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.height,
                            imageProxy.width,
                            isImageFlipped
                        )
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false
                }
                try {
                    Log.e(TAG, "try")
                    faceDetectionProcessor!!.processImageProxy(imageProxy, graphicOverlay)
                } catch (e: MlKitException) {
                    Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )

        setContent {
            MultimediaHWTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (analysisUseCase != null)
                        CameraView(CameraViewModel(), analysisUseCase!!, graphicOverlay!!)
                    else
                        Toast.makeText(this, "Analysis usecase not set",
                            Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "CameraXLivePreview"
    }
}