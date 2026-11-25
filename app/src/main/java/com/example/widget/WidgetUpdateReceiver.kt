package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                HabitWidget().updateAll(context)
                Log.d("WidgetUpdateReceiver", "Successfully updated HabitWidget via pre-reminder trigger")
            } catch (e: Exception) {
                Log.e("WidgetUpdateReceiver", "Failed to update HabitWidget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
