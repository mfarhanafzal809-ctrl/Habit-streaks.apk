#!/bin/bash

# Add imports
sed -i '/import androidx.compose.material3.Text/a import androidx.compose.material3.DrawerValue\nimport androidx.compose.material3.ModalNavigationDrawer\nimport androidx.compose.material3.rememberDrawerState\nimport androidx.compose.runtime.rememberCoroutineScope\nimport kotlinx.coroutines.launch\nimport com.example.ui.components.AppDrawer\nimport com.example.ui.screens.ContactScreen\nimport com.example.ui.components.ThemeCustomizationBottomSheet' app/src/main/java/com/example/MainActivity.kt

