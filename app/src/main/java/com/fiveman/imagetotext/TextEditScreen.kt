package com.fiveman.imagetotext

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import androidx.navigation.NavController
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditScreen(
    navController: NavController,
    initialText: String
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val decodedText = URLDecoder.decode(initialText, StandardCharsets.UTF_8.toString())
    var editableText by remember { mutableStateOf(decodedText) }
    var showSaveDialog by remember { mutableStateOf(false) }
    //var isSwitchChecked by remember { mutableStateOf(false) }

    // States for the Save Dialog
    var fileName by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(FileFormat.TXT) }

    val textFieldScrollState = rememberScrollState()

    // ActivityResultLauncher for SAF (Storage Access Framework)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedFormat.mimeType)
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    when (selectedFormat) {
                        FileFormat.PDF -> {
                            // Use iText to create a PDF
                            val pdfWriter = PdfWriter(outputStream)
                            val pdfDocument = PdfDocument(pdfWriter)
                            val document = Document(pdfDocument)
                            document.add(Paragraph(editableText))
                            document.close()
                            Log.d("TextEditScreen", "PDF saved with ${editableText.length} characters")
                        }
                /*        FileFormat.DOC -> {
                            // Use Apache POI to create a .docx file
                            val doc = XWPFDocument()
                            // Create a single paragraph for simplicity
                            val paragraph = doc.createParagraph()
                            val run = paragraph.createRun()
                            // Handle special characters and large text
                            run.setText(editableText.replace("\r", "").replace("\n", "\r\n"))
                            doc.write(outputStream)
                            outputStream.flush()
                            doc.close()
                            Log.d("TextEditScreen", "DOCX saved with ${editableText.length} characters")
                        }*/
                        FileFormat.TXT -> {
                            // Write text directly for TXT
                            outputStream.write(editableText.toByteArray(StandardCharsets.UTF_8))
                            outputStream.flush()
                            Log.d("TextEditScreen", "TXT saved with ${editableText.length} characters")
                        }
                    }
                }
                Toast.makeText(context, "File saved successfully!", Toast.LENGTH_LONG).show()

                // Attempt to open the file
                try {
/*                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, selectedFormat.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        // Fallback MIME type for .docx
                        if (selectedFormat == FileFormat.DOC) {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            setDataAndType(uri, "application/msword")
                        }
                    }
                    context.startActivity(Intent.createChooser(intent, "Open ${selectedFormat.displayName}"))*/
                } catch (e: Exception) {
                    Log.e("TextEditScreen", "Failed to open file: ${e.message}")
                    Toast.makeText(context, "Unable to open file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("TextEditScreen", "Failed to save file: ${e.message}")
                e.printStackTrace()
                Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "File save cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanned Text") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                // Copy Button
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(editableText))
                        Toast.makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    Text("Copy", style = MaterialTheme.typography.labelSmall)
                }

                // Share Button
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, editableText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    Text("Share", style = MaterialTheme.typography.labelSmall)
                }

                // Save Button
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = {
                        // Generate a default filename
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        fileName = "ImageToText_$timestamp${selectedFormat.extension}"
                        showSaveDialog = true
                    }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Save")
                    }
                    Text("Save", style = MaterialTheme.typography.labelSmall)
                }

                // Translate Button
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = {
                        Toast.makeText(context, "Translate feature coming soon!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Build, contentDescription = "Translate")
                    }
                    Text("Translate", style = MaterialTheme.typography.labelSmall)
                }

                // Switch Option
               /* Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Switch(
                        checked = isSwitchChecked,
                        onCheckedChange = {
                            isSwitchChecked = it
                            Toast.makeText(context, "Switch Toggled: $isSwitchChecked", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    )
                    Text("Option", style = MaterialTheme.typography.labelSmall)
                }*/
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                            .verticalScroll(textFieldScrollState),
                        placeholder = { Text("Your extracted text will appear here...") },
                        maxLines = Int.MAX_VALUE,
                        singleLine = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }

    // Save As File Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Text As File") },
            text = {
                Column {
                    Text("Suggested Path: Downloads / Documents (User selectable)")
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { newValue ->
                            val baseName = newValue.substringBeforeLast('.', newValue)
                            fileName = baseName + selectedFormat.extension
                        },
                        label = { Text("File Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // File Format Options
                    FileFormat.entries.forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFormat = format
                                    val baseName = fileName.substringBeforeLast('.', fileName)
                                    fileName = baseName + selectedFormat.extension
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedFormat == format,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        selectedFormat = format
                                        val baseName = fileName.substringBeforeLast('.', fileName)
                                        fileName = baseName + selectedFormat.extension
                                    }
                                }
                            )
                            Text(format.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    createDocumentLauncher.launch(fileName)
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Enum for file formats
enum class FileFormat(val extension: String, val mimeType: String, val displayName: String) {
    TXT(".txt", "text/plain", "Text (.txt)"),
    PDF(".pdf", "application/pdf", "PDF (.pdf)"),
   // DOC(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX (.docx)")
}