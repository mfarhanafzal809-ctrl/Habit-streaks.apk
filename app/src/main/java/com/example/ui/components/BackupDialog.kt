package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BackupDialog(
    onExportFile: suspend () -> File,
    onExportCsvFile: (suspend () -> File)? = null,
    onImportStream: suspend (java.io.InputStream, Boolean) -> Result<String>,
    isAdFreeActive: Boolean = false,
    onRequestExportAd: ((() -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var replaceExisting by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var showReplaceConfirmation by remember { mutableStateOf(false) }

    fun performJsonExport() {
        scope.launch {
            isProcessing = true
            try {
                val file = onExportFile()
                statusMessage = "Backup saved to: ${file.name}"
                isError = false

                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Habit Streaks Backup"))
                } catch (_: ActivityNotFoundException) {
                    statusMessage = "Backup saved to app files (${file.name}), no share app found."
                    isError = false
                }
            } catch (e: Exception) {
                statusMessage = "Export failed: ${e.message}"
                isError = true
            } finally {
                isProcessing = false
            }
        }
    }

    fun performCsvExport() {
        if (onExportCsvFile == null) return
        scope.launch {
            isProcessing = true
            try {
                val file = onExportCsvFile.invoke()
                statusMessage = "Spreadsheet exported: ${file.name}"
                isError = false

                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Habit Spreadsheet"))
                } catch (_: ActivityNotFoundException) {
                    statusMessage = "Spreadsheet saved (${file.name}), no share app found."
                    isError = false
                }
            } catch (e: Exception) {
                statusMessage = "CSV export failed: ${e.message}"
                isError = true
            } finally {
                isProcessing = false
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val result = onImportStream(inputStream, replaceExisting)
                        if (result.isSuccess) {
                            statusMessage = result.getOrNull()
                            isError = false
                        } else {
                            statusMessage = result.exceptionOrNull()?.message ?: "Failed to import file"
                            isError = true
                        }
                    } else {
                        statusMessage = "Could not open selected file."
                        isError = true
                    }
                } catch (e: Exception) {
                    statusMessage = "Error importing: ${e.message}"
                    isError = true
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    if (showReplaceConfirmation) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirmation = false },
            title = { Text("Confirm Replace All Data") },
            text = {
                Text("Replacing all data will permanently delete all existing local habits, completions, and streak records before importing the new file. This action cannot be undone.\n\nDo you want to proceed and select a backup file?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReplaceConfirmation = false
                        try {
                            filePicker.launch(
                                arrayOf(
                                    "application/json",
                                    "text/json",
                                    "text/plain",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        } catch (_: ActivityNotFoundException) {
                            statusMessage = "No file picker available on this device."
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Proceed with Replace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Export your habit history to JSON backup or CSV spreadsheet, or restore from a previous backup file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Export Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Export Data & Spreadsheets",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            PremiumFeatureBadge(
                                requiredAdsLabel = "1 Ad",
                                isAdFreeActive = isAdFreeActive,
                                compact = true
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Backup all habits & completions, or export CSV to open in Excel / Sheets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (onRequestExportAd != null) {
                                        onRequestExportAd { performJsonExport() }
                                    } else {
                                        performJsonExport()
                                    }
                                },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON Backup", fontSize = 12.sp)
                            }

                            if (onExportCsvFile != null) {
                                OutlinedButton(
                                    onClick = {
                                        if (onRequestExportAd != null) {
                                            onRequestExportAd { performCsvExport() }
                                        } else {
                                            performCsvExport()
                                        }
                                    },
                                    enabled = !isProcessing,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CSV Sheet", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Import Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Import / Restore Backup",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Import strategy options
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !replaceExisting,
                                onClick = { replaceExisting = false }
                            )
                            Text("Merge with current data", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = replaceExisting,
                                onClick = { replaceExisting = true }
                            )
                            Text("Replace all data", style = MaterialTheme.typography.bodySmall)
                        }

                        if (replaceExisting) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Replacing will permanently erase all current habits and streak history before restoring.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                if (replaceExisting) {
                                    showReplaceConfirmation = true
                                } else {
                                    try {
                                        filePicker.launch(
                                            arrayOf(
                                                "application/json",
                                                "text/json",
                                                "text/plain",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    } catch (_: ActivityNotFoundException) {
                                        statusMessage = "No file picker available on this device."
                                        isError = true
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Backup JSON File")
                        }
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
