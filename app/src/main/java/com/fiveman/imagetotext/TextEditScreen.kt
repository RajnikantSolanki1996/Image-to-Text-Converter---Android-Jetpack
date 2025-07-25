// TextEditScreen.kt
package com.fiveman.imagetotext

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.content.Intent
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ThumbUp
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditScreen(
    navController: NavController,
    initialText: String
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    
    // Decode the text
    val decodedText = URLDecoder.decode(initialText, StandardCharsets.UTF_8.toString())
    var editableText by remember { mutableStateOf(decodedText) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Edit Extracted Text") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Copy button
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(editableText))
                    }
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = "Copy")
                }
                
                // Share button
                IconButton(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, editableText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share text"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                
                // Save button
                IconButton(
                    onClick = { showSaveDialog = true }
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Save")
                }
            }
        )
        
        // Text editing area
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Extracted Text (Editable):",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = editableText,
                    onValueChange = { editableText = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    placeholder = { Text("Your extracted text will appear here...") },
                    maxLines = Int.MAX_VALUE,
                    singleLine = false
                )
            }
        }
    }
    
    // Save confirmation dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Text") },
            text = { Text("Text has been saved to clipboard. You can also use the share button to save to other apps.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(editableText))
                        showSaveDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}