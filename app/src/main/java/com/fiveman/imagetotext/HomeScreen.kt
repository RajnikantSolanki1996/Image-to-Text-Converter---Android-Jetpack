package com.fiveman.imagetotext

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher - declare first
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            val encodedUri = URLEncoder.encode(tempImageUri.toString(), StandardCharsets.UTF_8.toString())
            navController.navigate("crop/$encodedUri")
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Create temp file for camera image
            val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            tempImageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            cameraLauncher.launch(tempImageUri)
        } else {
            showPermissionDialog = true
        }
    }

    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val encodedUri = URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString())
            navController.navigate("crop/$encodedUri")
        }
    }
    
    // PDF launcher
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val encodedUri = URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString())
            navController.navigate("crop/$encodedUri")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Image to Text",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // Camera Option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                        tempImageUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                        cameraLauncher.launch(tempImageUri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Camera",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Take Photo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Gallery Option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Button(
                onClick = {
                    galleryLauncher.launch("image/*")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Gallery",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Choose from Gallery",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // PDF Scan Option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Button(
                onClick = {
                    pdfLauncher.launch("application/pdf")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(
                    Icons.Default.ThumbUp,
                    contentDescription = "PDF Scan",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Scan PDF",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    
    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Camera Permission Required") },
            text = { Text("This app needs camera permission to take photos. Please grant the permission in app settings.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}