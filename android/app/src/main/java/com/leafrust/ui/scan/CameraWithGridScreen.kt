package com.leafrust.ui.scan

import android.Manifest
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.leafrust.ui.theme.LeafGreen
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraWithGridScreen(
    onCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { p ->
                p.setSurfaceProvider(previewView.surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                )
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )

            LeafCaptureGrid(modifier = Modifier.fillMaxSize())

            Text(
                text = "Расположите лист в рамке по сетке",
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                        .padding(6.dp)
                        .background(if (capturing) Color.Gray else LeafGreen, CircleShape)
                        .clickable(enabled = !capturing) {
                            val capture = imageCapture ?: return@clickable
                            capturing = true
                            val photoFile = File(context.cacheDir, "leaf_${System.currentTimeMillis()}.jpg")
                            val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            capture.takePicture(
                                output,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            photoFile,
                                        )
                                        ContextCompat.getMainExecutor(context).execute {
                                            capturing = false
                                            onCaptured(uri)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            capturing = false
                                        }
                                    }
                                },
                            )
                        },
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Снять", color = Color.White)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нужен доступ к камере", color = Color.White)
            }
        }
    }
}

@Composable
fun LeafCaptureGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val lineColor = Color.White.copy(alpha = 0.55f)
        val accent = LeafGreen.copy(alpha = 0.85f)

        drawLine(lineColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 2f)
        drawLine(lineColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth = 2f)
        drawLine(lineColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 2f)
        drawLine(lineColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth = 2f)

        val frame = leafFrameIn(size)
        val left = frame.left
        val top = frame.top
        val frameW = frame.width
        val frameH = frame.height
        val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
        drawRoundRect(
            color = accent,
            topLeft = Offset(left, top),
            size = Size(frameW, frameH),
            cornerRadius = CornerRadius(48f, 48f),
            style = Stroke(width = 5f, pathEffect = dash),
        )

        val mark = 36f
        val corners = listOf(
            Offset(left, top),
            Offset(left + frameW, top),
            Offset(left, top + frameH),
            Offset(left + frameW, top + frameH),
        )
        drawLine(accent, corners[0], corners[0] + Offset(mark, 0f), strokeWidth = 6f)
        drawLine(accent, corners[0], corners[0] + Offset(0f, mark), strokeWidth = 6f)
        drawLine(accent, corners[1], corners[1] + Offset(-mark, 0f), strokeWidth = 6f)
        drawLine(accent, corners[1], corners[1] + Offset(0f, mark), strokeWidth = 6f)
        drawLine(accent, corners[2], corners[2] + Offset(mark, 0f), strokeWidth = 6f)
        drawLine(accent, corners[2], corners[2] + Offset(0f, -mark), strokeWidth = 6f)
        drawLine(accent, corners[3], corners[3] + Offset(-mark, 0f), strokeWidth = 6f)
        drawLine(accent, corners[3], corners[3] + Offset(0f, -mark), strokeWidth = 6f)
    }
}
