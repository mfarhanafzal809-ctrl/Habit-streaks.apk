package com.example.util

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class ErrorSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

data class EnterpriseErrorEvent(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
    val throwable: Throwable? = null,
    val canRetry: Boolean = true,
    val onRetry: (() -> Unit)? = null
)

object EnterpriseErrorHandler {
    private const val TAG = "EnterpriseErrorHandler"

    private val _errorFlow = MutableSharedFlow<EnterpriseErrorEvent>(extraBufferCapacity = 64)
    val errorFlow: SharedFlow<EnterpriseErrorEvent> = _errorFlow.asSharedFlow()

    private val originalDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun initGlobalHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
            try {
                _errorFlow.tryEmit(
                    EnterpriseErrorEvent(
                        title = "System Recovery Notice",
                        message = "An unexpected exception occurred: ${throwable.localizedMessage ?: throwable.javaClass.simpleName}. The system has safely recovered.",
                        severity = ErrorSeverity.CRITICAL,
                        throwable = throwable,
                        canRetry = false
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to emit critical error event", e)
            }
            try {
                originalDefaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {}
        }
    }

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Captured unhandled coroutine exception", throwable)
        reportError(
            title = "Background Operation Error",
            message = throwable.localizedMessage ?: "An asynchronous background task encountered an issue.",
            severity = ErrorSeverity.ERROR,
            throwable = throwable
        )
    }

    fun reportError(
        title: String,
        message: String,
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        throwable: Throwable? = null,
        canRetry: Boolean = true,
        onRetry: (() -> Unit)? = null
    ) {
        if (throwable != null) {
            Log.e(TAG, "[$severity] $title: $message", throwable)
        } else {
            Log.w(TAG, "[$severity] $title: $message")
        }
        _errorFlow.tryEmit(
            EnterpriseErrorEvent(
                title = title,
                message = message,
                severity = severity,
                throwable = throwable,
                canRetry = canRetry,
                onRetry = onRetry
            )
        )
    }

    suspend fun <T> executeWithGuards(
        actionName: String,
        fallbackValue: T,
        block: suspend () -> T
    ): T {
        return try {
            block()
        } catch (e: Throwable) {
            Log.e(TAG, "Guarded execution failed for '$actionName'", e)
            reportError(
                title = "Operation Failed",
                message = "Failed to execute $actionName: ${e.localizedMessage ?: e.javaClass.simpleName}",
                severity = ErrorSeverity.ERROR,
                throwable = e
            )
            fallbackValue
        }
    }
}
