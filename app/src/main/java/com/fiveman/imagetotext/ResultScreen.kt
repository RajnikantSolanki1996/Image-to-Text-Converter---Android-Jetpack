package com.fiveman.imagetotext

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(imageUri: String, navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val decodedUri = URLDecoder.decode(imageUri, "UTF-8")
    
    var extractedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    // Extract text from full image
    LaunchedEffect(decodedUri) {
        try {
            val uri = Uri.parse(decodedUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            val loadedBitmap = BitmapFactory.decodeStream(inputStream)
            bitmap = loadedBitmap
            
            if (loadedBitmap != null) {
                val image = InputImage.fromBitmap(loadedBitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        extractedText = visionText.text
                        isLoading = false
                        Log.d("ResultScreen", "Text extracted: ${visionText.text}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ResultScreen", "Text recognition failed", e)
                        extractedText = "Failed to extract text from image"
                        isLoading = false
                    }
            } else {
                extractedText = "Failed to load image"
                isLoading = false
            }
        } catch (e: Exception) {
            Log.e("ResultScreen", "Error processing image", e)
            extractedText = "Error processing image: ${e.message}"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Extracted Text",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // Image Preview
        bitmap?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Loading or Text Result
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Extracting text from image...")
                }
            }
        } else {
            // Text Result Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extracted Text:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(extractedText))
                                    showCopiedSnackbar = true
                                }
                            ) {
                                Icon(Icons.Default.AccountBox, contentDescription = "Copy")
                            }
                            IconButton(onClick = { /* Add share functionality */ }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = if (extractedText.isNotEmpty()) extractedText else "No text found in image",
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    // Snackbar for copy confirmation
    if (showCopiedSnackbar) {
        LaunchedEffect(showCopiedSnackbar) {
            kotlinx.coroutines.delay(2000)
            showCopiedSnackbar = false
        }
    }
}