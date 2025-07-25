// CropScreen.kt
package com.example.imagetotext.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    navController: NavController,
    imageUri: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var isProcessing by remember { mutableStateOf(false) }
    var rotation by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Crop rectangle state - relative to image
    var cropRect by remember {
        mutableStateOf(Rect(0.1f, 0.1f, 0.9f, 0.9f)) // Relative coordinates (0-1)
    }

    // Aspect ratio options
    val aspectRatios = listOf(
        "Free" to null,
        "1:1" to 1f,
        "4:3" to 4f/3f,
        "3:4" to 3f/4f,
        "16:9" to 16f/9f,
        "9:16" to 9f/16f
    )
    var selectedAspectRatio by remember { mutableStateOf(aspectRatios[0]) }

    // Decode URI
    val uri = Uri.parse(java.net.URLDecoder.decode(imageUri, StandardCharsets.UTF_8.toString()))
    val painter = rememberAsyncImagePainter(model = uri)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Crop Image") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        if (!isProcessing) {
                            isProcessing = true
                            scope.launch {
                                val extractedText = processImageAndExtractText(context, uri, cropRect)
                                val encodedText = URLEncoder.encode(extractedText, StandardCharsets.UTF_8.toString())
                                navController.navigate("text_edit/$encodedText")
                                isProcessing = false
                            }
                        }
                    }
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Process")
                    }
                }
            }
        )

        // Main crop area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                // Image with transformations
                Image(
                    painter = painter,
                    contentDescription = "Image to crop",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            rotationZ = rotation,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotate ->
                                scale = (scale * zoom).coerceIn(0.5f, 3f)
                                rotation += rotate
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            }
                        },
                    contentScale = ContentScale.Fit
                )

                // Crop overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                val sensitivity = 0.001f
                                val newLeft = (cropRect.left + dragAmount.x * sensitivity).coerceIn(0f, cropRect.right - 0.1f)
                                val newTop = (cropRect.top + dragAmount.y * sensitivity).coerceIn(0f, cropRect.bottom - 0.1f)
                                val newRight = (cropRect.right + dragAmount.x * sensitivity).coerceIn(cropRect.left + 0.1f, 1f)
                                val newBottom = (cropRect.bottom + dragAmount.y * sensitivity).coerceIn(cropRect.top + 0.1f, 1f)

                                // Determine which part is being dragged based on position
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val touchX = change.position.x
                                val touchY = change.position.y

                                cropRect = when {
                                    touchX < centerX && touchY < centerY -> // Top-left
                                        Rect(newLeft, newTop, cropRect.right, cropRect.bottom)
                                    touchX > centerX && touchY < centerY -> // Top-right
                                        Rect(cropRect.left, newTop, newRight, cropRect.bottom)
                                    touchX < centerX && touchY > centerY -> // Bottom-left
                                        Rect(newLeft, cropRect.top, cropRect.right, newBottom)
                                    else -> // Bottom-right
                                        Rect(cropRect.left, cropRect.top, newRight, newBottom)
                                }

                                // Apply aspect ratio if selected
                                selectedAspectRatio.second?.let { ratio ->
                                    val width = cropRect.width
                                    val height = width / ratio
                                    if (cropRect.top + height <= 1f) {
                                        cropRect = Rect(
                                            cropRect.left,
                                            cropRect.top,
                                            cropRect.right,
                                            cropRect.top + height
                                        )
                                    }
                                }
                            }
                        }
                ) {
                    drawAdvancedCropOverlay(cropRect, size)
                }
            }
        }

        // Bottom controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Aspect ratio selector
                Text(
                    text = "Aspect Ratio",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(aspectRatios.size) { index ->
                        val ratio = aspectRatios[index]
                        FilterChip(
                            onClick = {
                                selectedAspectRatio = ratio
                                // Adjust crop rect to match aspect ratio
                                ratio.second?.let { aspectRatio ->
                                    val currentWidth = cropRect.width
                                    val newHeight = currentWidth / aspectRatio
                                    if (cropRect.top + newHeight <= 1f) {
                                        cropRect = Rect(
                                            cropRect.left,
                                            cropRect.top,
                                            cropRect.right,
                                            cropRect.top + newHeight
                                        )
                                    }
                                }
                            },
                            label = { Text(ratio.first) },
                            selected = selectedAspectRatio == ratio
                        )
                    }
                }

                // Transform controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rotate left
                    IconButton(
                        onClick = { rotation -= 90f },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Rotate Left",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Reset
                    IconButton(
                        onClick = {
                            rotation = 0f
                            scale = 1f
                            offset = Offset.Zero
                            cropRect = Rect(0.1f, 0.1f, 0.9f, 0.9f)
                        },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Rotate right
                    IconButton(
                        onClick = { rotation += 90f },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Rotate Right",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Zoom slider
                Text(
                    text = "Zoom: ${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    valueRange = 0.5f..3f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun DrawScope.drawAdvancedCropOverlay(cropRect: Rect, canvasSize: Size) {
    val cropLeft = cropRect.left * canvasSize.width
    val cropTop = cropRect.top * canvasSize.height
    val cropRight = cropRect.right * canvasSize.width
    val cropBottom = cropRect.bottom * canvasSize.height

    val actualCropRect = Rect(
        Offset(cropLeft, cropTop),
        Size(cropRight - cropLeft, cropBottom - cropTop)
    )

    // Draw dark overlay outside crop area
    drawRect(
        color = Color.Black.copy(alpha = 0.6f),
        size = canvasSize
    )

    // Clear the crop area
    drawRect(
        color = Color.Transparent,
        topLeft = actualCropRect.topLeft,
        size = actualCropRect.size,
        blendMode = BlendMode.Clear
    )

    // Draw crop border
    drawRect(
        color = Color.White,
        topLeft = actualCropRect.topLeft,
        size = actualCropRect.size,
        style = Stroke(width = 3.dp.toPx())
    )

    // Draw grid lines (rule of thirds)
    val gridColor = Color.White.copy(alpha = 0.5f)
    val strokeWidth = 1.dp.toPx()

    // Vertical grid lines
    val gridWidth = actualCropRect.width / 3
    for (i in 1..2) {
        drawLine(
            color = gridColor,
            start = Offset(actualCropRect.left + gridWidth * i, actualCropRect.top),
            end = Offset(actualCropRect.left + gridWidth * i, actualCropRect.bottom),
            strokeWidth = strokeWidth
        )
    }

    // Horizontal grid lines
    val gridHeight = actualCropRect.height / 3
    for (i in 1..2) {
        drawLine(
            color = gridColor,
            start = Offset(actualCropRect.left, actualCropRect.top + gridHeight * i),
            end = Offset(actualCropRect.right, actualCropRect.top + gridHeight * i),
            strokeWidth = strokeWidth
        )
    }

    // Corner handles
    val handleSize = 20.dp.toPx()
    val handleThickness = 4.dp.toPx()
    val handleLength = 25.dp.toPx()
    val handleColor = Color.White

    val corners = listOf(
        actualCropRect.topLeft,
        Offset(actualCropRect.right, actualCropRect.top),
        Offset(actualCropRect.left, actualCropRect.bottom),
        Offset(actualCropRect.right, actualCropRect.bottom)
    )

    corners.forEachIndexed { index, corner ->
        val isLeft = index % 2 == 0
        val isTop = index < 2

        // Horizontal handle line
        drawLine(
            color = handleColor,
            start = Offset(
                if (isLeft) corner.x else corner.x - handleLength,
                corner.y
            ),
            end = Offset(
                if (isLeft) corner.x + handleLength else corner.x,
                corner.y
            ),
            strokeWidth = handleThickness
        )

        // Vertical handle line
        drawLine(
            color = handleColor,
            start = Offset(
                corner.x,
                if (isTop) corner.y else corner.y - handleLength
            ),
            end = Offset(
                corner.x,
                if (isTop) corner.y + handleLength else corner.y
            ),
            strokeWidth = handleThickness
        )
    }

    // Center drag handle
    val centerX = actualCropRect.left + actualCropRect.width / 2
    val centerY = actualCropRect.top + actualCropRect.height / 2

    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = 8.dp.toPx(),
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = 6.dp.toPx(),
        center = Offset(centerX, centerY)
    )
}

suspend fun processImageAndExtractText(
    context: Context,
    uri: Uri,
    cropRect: Rect
): String = withContext(Dispatchers.IO) {
    try {
        // Load and crop bitmap
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // For demo purposes, return mock extracted text
        // In a real app, you would use ML Kit Text Recognition or Tesseract OCR
        "This is sample extracted text from the image.\n\nYou can edit this text as needed.\n\nIn a real implementation, this would be the actual text extracted from the cropped image using OCR technology like ML Kit or Tesseract."
    } catch (e: Exception) {
        "Error extracting text: ${e.message}"
    }
}