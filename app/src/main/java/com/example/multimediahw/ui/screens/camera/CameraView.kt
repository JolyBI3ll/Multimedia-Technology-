package com.example.multimediahw.ui.screens.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.multimediahw.R
import com.example.multimediahw.detector.GraphicOverlay
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraView(
    viewModel: CameraViewModel,
    analysisUseCase: ImageAnalysis,
    graphicOverlay: GraphicOverlay
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(viewModel.lensFacing.collectAsState().value)
        .build()


    suspend fun Context.getCameraProvider(): ProcessCameraProvider =
        suspendCoroutine { continuation ->
            ProcessCameraProvider.getInstance(this).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, ContextCompat.getMainExecutor(this))
            }
        }


    LaunchedEffect(viewModel.lensFacing.collectAsState().value) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            analysisUseCase,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
            AndroidView({ graphicOverlay }, modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 20.dp,
                        start = 20.dp,
                        end = 20.dp
                    ),
                horizontalArrangement = Arrangement.SpaceAround
            ) {

                IconButton(
                    onClick = {
                        viewModel.changeFacing()
                    },
                    content = {
                        Icon(
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = 0.4f))
                                .padding(10.dp),
                            painter = painterResource(id = R.drawable.ic_rotate),
                        )
                    }
                )
            }
        }
}