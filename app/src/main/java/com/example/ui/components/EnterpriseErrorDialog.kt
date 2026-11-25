package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.util.EnterpriseErrorEvent
import com.example.util.EnterpriseErrorHandler
import com.example.util.ErrorSeverity
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EnterpriseErrorDialogHost() {
    var currentError by remember { mutableStateOf<EnterpriseErrorEvent?>(null) }

    LaunchedEffect(Unit) {
        EnterpriseErrorHandler.errorFlow.collectLatest { event ->
            // Only popup dialog for ERROR or CRITICAL severity to avoid intrusive noise
            if (event.severity == ErrorSeverity.ERROR || event.severity == ErrorSeverity.CRITICAL) {
                currentError = event
            }
        }
    }

    currentError?.let { error ->
        AlertDialog(
            onDismissRequest = { currentError = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = if (error.severity == ErrorSeverity.CRITICAL) Icons.Default.Error else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (error.severity == ErrorSeverity.CRITICAL) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    if (error.canRetry && error.onRetry != null) {
                        Button(
                            onClick = {
                                val action = error.onRetry
                                currentError = null
                                action?.invoke()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                    OutlinedButton(
                        onClick = { currentError = null },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }
}
