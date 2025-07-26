package com.fiveman.imagetotext.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    navController: NavController,
    imageUri: String
) {
    val context = LocalContext.current
    val decodedUri = remember(imageUri) {
        Uri.parse(URLDecoder.decode(imageUri, Charsets.UTF_8.name()))
    }

    val coroutineScope = rememberCoroutineScope()
    var isCropping by remember { mutableStateOf(true) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isCropping = false
        if (result.resultCode == Activity.RESULT_OK) {
            val croppedUri = UCrop.getOutput(result.data!!)
            croppedUri?.let {
                coroutineScope.launch {
                    val extractedText = extractTextFromImage(context, it)
                    val encodedText = URLEncoder.encode(extractedText, Charsets.UTF_8.name())
                    navController.navigate("text_edit/$encodedText") {
                        popUpTo("crop/{imageUri}") { inclusive = true }
                    }
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            UCrop.getError(result.data!!)?.printStackTrace()
        }
    }

    // Launch uCrop when screen is first shown
    LaunchedEffect(Unit) {
        isCropping = true
        startCropWithUCrop(context, decodedUri, cropLauncher)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop Image") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.zIndex(2f) // Ensure TopAppBar is always on top
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (isCropping) {
                    CircularProgressIndicator()
                } else {
                    Text("Cropping complete or canceled.")
                }
            }
        }
    )
}

// OCR logic
suspend fun extractTextFromImage(context: Context, imageUri: Uri): String = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: return@withContext "Error: Cannot open image"
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(image).await()

        result.text.ifEmpty { "No text found in image." }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

// UCrop launcher
fun startCropWithUCrop(
    context: Context,
    sourceUri: Uri,
    cropLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val destinationUri = Uri.fromFile(
        File(context.cacheDir, "cropped_${UUID.randomUUID()}.jpg")
    )

    val uCrop = UCrop.of(sourceUri, destinationUri)
        .withAspectRatio(1f, 1f)
        .withMaxResultSize(1000, 1000)
        .withOptions(getUCropOptions(context))

    cropLauncher.launch(uCrop.getIntent(context))
}

// UCrop styling
fun getUCropOptions(context: Context): UCrop.Options {
    return UCrop.Options().apply {
        setCompressionFormat(Bitmap.CompressFormat.JPEG)
        setCompressionQuality(90)
        setFreeStyleCropEnabled(true)
        setToolbarColor(androidx.compose.ui.graphics.Color.White.toArgb())
        setStatusBarColor(androidx.compose.ui.graphics.Color.White.toArgb())
        setActiveControlsWidgetColor(androidx.compose.ui.graphics.Color.Black.toArgb())
        setToolbarWidgetColor(androidx.compose.ui.graphics.Color.Black.toArgb())
    }
}
