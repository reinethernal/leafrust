package com.leafrust.ui.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.leafrust.ui.components.PrimaryButton
import com.leafrust.ui.theme.LeafGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun AlignLeafScreen(
    sourceUri: Uri,
    onAligned: (Uri) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var viewW by remember { mutableFloatStateOf(0f) }
    var viewH by remember { mutableFloatStateOf(0f) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var baseScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            try {
                bitmap = loadOrientedBitmap(context, sourceUri)
                loadError = null
            } catch (e: Exception) {
                loadError = e.message ?: "Не удалось открыть фото"
            }
        }
    }

    LaunchedEffect(bitmap, viewW, viewH) {
        val bmp = bitmap ?: return@LaunchedEffect
        if (viewW <= 0f || viewH <= 0f) return@LaunchedEffect
        baseScale = min(viewW / bmp.width.toFloat(), viewH / bmp.height.toFloat())
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        rotation = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged {
                viewW = it.width.toFloat()
                viewH = it.height.toFloat()
            },
    ) {
        val bmp = bitmap
        if (bmp != null && viewW > 0f && viewH > 0f) {
            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
            val drawW = bmp.width * baseScale
            val drawH = bmp.height * baseScale

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX
                        translationY = offsetY
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    },
            ) {
                val left = ((size.width - drawW) / 2f).roundToInt()
                val top = ((size.height - drawH) / 2f).roundToInt()
                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(left, top),
                    dstSize = IntSize(
                        drawW.roundToInt().coerceAtLeast(1),
                        drawH.roundToInt().coerceAtLeast(1),
                    ),
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val frame = leafFrameIn(size)
                val dim = Color.Black.copy(alpha = 0.48f)
                drawRect(dim, Offset.Zero, Size(size.width, frame.top))
                drawRect(dim, Offset(0f, frame.bottom), Size(size.width, size.height - frame.bottom))
                drawRect(dim, Offset(0f, frame.top), Size(frame.left, frame.height))
                drawRect(dim, Offset(frame.right, frame.top), Size(size.width - frame.right, frame.height))
            }
            LeafCaptureGrid(modifier = Modifier.fillMaxSize())

            // Gesture layer above overlays so pan/zoom/rotate always work
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotationChange ->
                            scale = (scale * zoom).coerceIn(0.35f, 8f)
                            offsetX += pan.x
                            offsetY += pan.y
                            rotation += Math.toDegrees(rotationChange.toDouble()).toFloat()
                        }
                    },
            )
        } else if (loadError != null) {
            Text(
                text = loadError ?: "",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        } else {
            CircularProgressIndicator(
                color = LeafGreen,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Text(
            text = "Масштаб, сдвиг и поворот — поместите лист в рамку",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundTool(Icons.Default.RotateLeft, "−90°") { rotation -= 90f }
                RoundTool(Icons.Default.Remove, "Отдалить") {
                    scale = (scale / 1.18f).coerceAtLeast(0.35f)
                }
                RoundTool(Icons.Default.Add, "Приблизить") {
                    scale = (scale * 1.18f).coerceAtMost(8f)
                }
                RoundTool(Icons.Default.RotateRight, "+90°") { rotation += 90f }
                RoundTool(Icons.Default.Refresh, "Сброс") {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                    rotation = 0f
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(
                label = if (busy) "Обрезаем…" else "Готово — анализировать",
                enabled = bmp != null && !busy && viewW > 0f,
                onClick = {
                    val source = bmp ?: return@PrimaryButton
                    busy = true
                    scope.launch {
                        try {
                            val uri = withContext(Dispatchers.Default) {
                                val cropped = cropToLeafFrame(
                                    source = source,
                                    viewW = viewW,
                                    viewH = viewH,
                                    baseScale = baseScale,
                                    userScale = scale,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    rotationDeg = rotation,
                                )
                                val file = File(
                                    context.cacheDir,
                                    "leaf_aligned_${System.currentTimeMillis()}.jpg",
                                )
                                FileOutputStream(file).use { out ->
                                    cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                }
                                if (cropped !== source) cropped.recycle()
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file,
                                )
                            }
                            onAligned(uri)
                        } catch (e: Exception) {
                            loadError = e.message ?: "Ошибка обрезки"
                            busy = false
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Жесты: щипок — масштаб, два пальца — поворот, сдвиг — панорама",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun RoundTool(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
    }
}

private fun loadOrientedBitmap(context: android.content.Context, uri: Uri): Bitmap {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }
    val maxSide = 2048
    var sample = 1
    val longest = max(bounds.outWidth, bounds.outHeight)
    while (longest / sample > maxSide) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri).use { input ->
        BitmapFactory.decodeStream(input, null, opts)
    } ?: error("Битый файл изображения")

    val orientation = try {
        resolver.openInputStream(uri).use { input ->
            if (input == null) ExifInterface.ORIENTATION_NORMAL
            else ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
    }
    return if (matrix.isIdentity) {
        decoded
    } else {
        val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (oriented !== decoded) decoded.recycle()
        oriented
    }
}

fun cropToLeafFrame(
    source: Bitmap,
    viewW: Float,
    viewH: Float,
    baseScale: Float,
    userScale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationDeg: Float,
): Bitmap {
    val vw = viewW.roundToInt().coerceAtLeast(2)
    val vh = viewH.roundToInt().coerceAtLeast(2)
    val frame = leafFrameIn(Size(viewW, viewH))
    val viewBmp = Bitmap.createBitmap(vw, vh, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(viewBmp)
    canvas.drawColor(android.graphics.Color.BLACK)

    val cx = viewW / 2f
    val cy = viewH / 2f
    val drawW = source.width * baseScale
    val drawH = source.height * baseScale
    val imgLeft = (viewW - drawW) / 2f
    val imgTop = (viewH - drawH) / 2f
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

    canvas.save()
    canvas.translate(cx + offsetX, cy + offsetY)
    canvas.rotate(rotationDeg)
    canvas.scale(userScale.coerceAtLeast(0.01f), userScale.coerceAtLeast(0.01f))
    canvas.translate(-cx, -cy)
    val dst = android.graphics.RectF(imgLeft, imgTop, imgLeft + drawW, imgTop + drawH)
    canvas.drawBitmap(source, null, dst, paint)
    canvas.restore()

    val left = frame.left.roundToInt().coerceIn(0, vw - 1)
    val top = frame.top.roundToInt().coerceIn(0, vh - 1)
    val width = frame.width.roundToInt().coerceIn(1, vw - left)
    val height = frame.height.roundToInt().coerceIn(1, vh - top)
    val cropped = Bitmap.createBitmap(viewBmp, left, top, width, height)
    viewBmp.recycle()

    val outW = 1024
    val outH = max(1, (outW * height.toFloat() / width.toFloat()).roundToInt())
    val scaled = Bitmap.createScaledBitmap(cropped, outW, outH, true)
    if (scaled !== cropped) cropped.recycle()
    return scaled
}
