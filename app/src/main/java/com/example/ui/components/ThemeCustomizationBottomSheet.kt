package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.getThemeColorScheme
import com.example.ui.theme.glassmorphicCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationBottomSheet(
    currentTemplate: String,
    isDarkMode: Boolean,
    isDynamicColor: Boolean,
    isDynamicRainbow: Boolean,
    savedCustomThemes: String,
    dynamicColorSource: String,
    isAdFreeActive: Boolean = false,
    onDarkModeToggle: (Boolean) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onDynamicRainbowToggle: (Boolean) -> Unit,
    onSaveCustomTheme: (String) -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onSetDynamicColorSource: (String) -> Unit,
    onTemplateSelect: (String) -> Unit,
    onApplyThemeWithModes: ((template: String, darkMode: Boolean, dynamicColor: Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
    onOpenGlassVfx: (() -> Unit)? = null
) {
    var selectedTemplate by remember { mutableStateOf(currentTemplate) }
    var localIsDark by remember { mutableStateOf(isDarkMode) }
    var localIsDynamic by remember { mutableStateOf(isDynamicColor) }
    var localIsRainbow by remember { mutableStateOf(isDynamicRainbow) }
    LaunchedEffect(currentTemplate, isDarkMode, isDynamicColor, isDynamicRainbow) {
        selectedTemplate = currentTemplate
        localIsDark = isDarkMode
        localIsDynamic = isDynamicColor
        localIsRainbow = isDynamicRainbow
    }
    var customColorInput by remember { mutableStateOf("") }
    var customThemeName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Built-in, 1: Custom

    val context = LocalContext.current

    val customColors = remember(customColorInput) {
        parseCustomColors(customColorInput)
    }

    val savedCustomList = remember(savedCustomThemes) {
        if (savedCustomThemes.isBlank()) emptyList() else savedCustomThemes.split(";;;")
    }
    
    val previewColorScheme = when {
        localIsDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (localIsDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        selectedTemplate.startsWith("CUSTOM:") -> {
            getThemeColorScheme(selectedTemplate, localIsDark)
        }
        else -> getThemeColorScheme(selectedTemplate, localIsDark)
    }

    val templates = listOf(
        ThemeOption("SUNSET_CORAL", "Sunset Coral (01)", Color(0xFFF97316), Color(0xFFFB923C)),
        ThemeOption("OCEAN_BLUE", "Ocean Blue (02)", Color(0xFF2563EB), Color(0xFF38BDF8)),
        ThemeOption("EMERALD_MINT", "Emerald Mint (03)", Color(0xFF059669), Color(0xFF34D399)),
        ThemeOption("GOLDEN_AMBER", "Golden Amber (04)", Color(0xFFD97706), Color(0xFFF59E0B)),
        ThemeOption("ORCHID_PURPLE", "Orchid Purple (05)", Color(0xFF7C3AED), Color(0xFFA855F7)),
        ThemeOption("LIME_CYAN", "Lime Cyan (06)", Color(0xFF16A34A), Color(0xFF06B6D4)),
        ThemeOption("INDIGO", "Electric Indigo", Color(0xFF4F46E5), Color(0xFF06B6D4)),
        ThemeOption("CYBERPUNK", "Cyberpunk Neon", Color(0xFFFF007F), Color(0xFF00F0FF)),
        ThemeOption("EMERALD", "Forest Emerald", Color(0xFF10B981), Color(0xFF34D399)),
        ThemeOption("ROSE", "Midnight Rose", Color(0xFFE11D48), Color(0xFFFB7185)),
        ThemeOption("SUNSET", "Amber Sunset", Color(0xFFD97706), Color(0xFFFBBF24)),
        ThemeOption("OCEAN", "Oceanic Blue", Color(0xFF0284C7), Color(0xFF38BDF8)),
        ThemeOption("ROYAL", "Royal Purple", Color(0xFF8B5CF6), Color(0xFFEC4899)),
        ThemeOption("AURORA", "Aurora Borealis", Color(0xFF0D9488), Color(0xFF65A30D)),
        ThemeOption("OBSIDIAN_GOLD", "Obsidian Gold", Color(0xFFFFD700), Color(0xFFD97706)),
        ThemeOption("MIDNIGHT_NEON", "Midnight Neon", Color(0xFF9333EA), Color(0xFFA3E635)),
        ThemeOption("CHERRY_BLOSSOM", "Sakura Blossom", Color(0xFFDB2777), Color(0xFFFDA4AF)),
        ThemeOption("COSMIC", "Cosmic Nebula", Color(0xFF6366F1), Color(0xFF9333EA)),
        ThemeOption("ORANGE", "Sunset Orange", Color(0xFFF97316), Color(0xFFFACC15)),
        ThemeOption("CRIMSON", "Crimson Velvet", Color(0xFFBE123C), Color(0xFFFDA4AF)),
        ThemeOption("MINT", "Alpine Mint", Color(0xFF059669), Color(0xFF2DD4BF)),
        ThemeOption("NEBULA_PURPLE", "Nebula Purple", Color(0xFFA855F7), Color(0xFFE879F9)),
        ThemeOption("VAPORWAVE", "Vaporwave Retro", Color(0xFFF43F5E), Color(0xFF22D3EE)),
        ThemeOption("NORDIC_FROST", "Nordic Frost", Color(0xFF67E8F9), Color(0xFF60A5FA)),
        ThemeOption("DESERT_DUNE", "Desert Dune", Color(0xFFFB923C), Color(0xFFFACC15)),
        ThemeOption("VOLCANIC_EMBER", "Volcanic Ember", Color(0xFFF87171), Color(0xFFFB923C)),
        ThemeOption("MATCHA_ZEN", "Matcha Zen", Color(0xFF4ADE80), Color(0xFFA3E635)),
        ThemeOption("DEEP_SPACE", "Deep Space", Color(0xFF818CF8), Color(0xFFFB7185)),
        ThemeOption("AMETHYST_LUX", "Amethyst Lux", Color(0xFFC084FC), Color(0xFFFBBF24)),
        ThemeOption("MONOCHROME_STEEL", "Monochrome Steel", Color(0xFF94A3B8), Color(0xFFCBD5E1))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "Theme & Live Preview",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    PremiumFeatureBadge(
                        requiredAdsLabel = if (selectedTemplate.startsWith("CUSTOM:")) "2 Ads" else "1 Ad",
                        isAdFreeActive = isAdFreeActive,
                        compact = true
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Customize palette with real-time UI preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                val applyAdsLabel = if (selectedTemplate.startsWith("CUSTOM:")) "2 Ads" else "1 Ad"
                Button(
                    onClick = {
                        if (onApplyThemeWithModes != null) {
                            onApplyThemeWithModes(selectedTemplate, localIsDark, localIsDynamic)
                        } else {
                            onDarkModeToggle(localIsDark)
                            onDynamicColorToggle(localIsDynamic)
                            onTemplateSelect(selectedTemplate)
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                        if (!isAdFreeActive) {
                            Text(
                                text = applyAdsLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // LIVE COLOR SCHEME PREVIEW CARD
            MaterialTheme(colorScheme = previewColorScheme) {
                val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isGlass) {
                                Modifier.glassmorphicCard(shape = RoundedCornerShape(20.dp), borderWidth = 1.6.dp)
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = com.example.ui.theme.glassmorphicCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = com.example.ui.theme.glassmorphicCardElevation(defaultElevation = 4.dp),
                    border = if (isGlass) null else com.example.ui.theme.glassmorphicCardBorder()
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
                                text = "Live Preview Card",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Primary Action", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = { },
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Secondary", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Display Mode & Dynamic Color Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { localIsDark = false },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!localIsDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!localIsDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Light")
                }

                Button(
                    onClick = { localIsDark = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (localIsDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (localIsDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.NightlightRound, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dark")
                }
            }

            if (onOpenGlassVfx != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenGlassVfx()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Open Glass Morphine & VFX Studio",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PremiumFeatureBadge(
                            requiredAdsLabel = "2 Ads",
                            isAdFreeActive = isAdFreeActive,
                            compact = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Dynamic Rainbow Auto-Transition",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            PremiumFeatureBadge(
                                requiredAdsLabel = "2 Ads",
                                isAdFreeActive = isAdFreeActive,
                                compact = true
                            )
                        }
                    }
                    Switch(
                        checked = localIsRainbow,
                        onCheckedChange = {
                            if (!it || isAdFreeActive) {
                                localIsRainbow = it
                            }
                            onDynamicRainbowToggle(it)
                        }
                    )
                }

                // Controls right below Dynamic Rainbow toggle, appearing only when dynamic rainbow is ON
                if (localIsRainbow) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Transition Source Pool:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("BUILTIN" to "Built-in", "CUSTOM" to "Custom", "BOTH" to "Both").forEach { (key, label) ->
                            val selected = dynamicColorSource == key
                            OutlinedButton(
                                onClick = { onSetDynamicColorSource(key) },
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // TWO CATEGORIES IN CUSTOM THEME PANEL: 1: Built-in, 2: Custom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("1: Built-in Palettes", "2: Custom Studio").forEachIndexed { index, title ->
                    val selected = selectedTab == index
                    Button(
                        onClick = { selectedTab = index },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // 1: Built-in Palettes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(templates, key = { it.id }) { option ->
                            ThemeCard(
                                option = option,
                                isSelected = selectedTemplate == option.id,
                                onClick = {
                                    selectedTemplate = option.id
                                }
                            )
                        }
                    }
                }
            } else {
                // 2: Custom Studio (Save, Delete, Manage Custom Combos)
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "✨ Custom Color Creator",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    PremiumFeatureBadge(
                                        requiredAdsLabel = "2 Ads",
                                        isAdFreeActive = isAdFreeActive,
                                        compact = true
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(customColors.first))
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(customColors.second))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter name & color (plain language like 'Sunset Orange' or Hex #6366F1). Save custom combos without applying, or apply instantly!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = customThemeName,
                                onValueChange = { customThemeName = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Custom Theme Name (e.g. My Neon)") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customColorInput,
                                onValueChange = { 
                                    customColorInput = it
                                    if (it.isNotBlank()) {
                                        val pArgb = android.graphics.Color.rgb((customColors.first.red * 255).toInt(), (customColors.first.green * 255).toInt(), (customColors.first.blue * 255).toInt())
                                        val sArgb = android.graphics.Color.rgb((customColors.second.red * 255).toInt(), (customColors.second.green * 255).toInt(), (customColors.second.blue * 255).toInt())
                                        val pHex = String.format("#%06X", pArgb and 0xFFFFFF)
                                        val sHex = String.format("#%06X", sArgb and 0xFFFFFF)
                                        selectedTemplate = "CUSTOM:$pHex|$sHex"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Color (e.g. #FF007F or 'Emerald Green')") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Save without applying
                                OutlinedButton(
                                    onClick = {
                                        if (customColorInput.isNotBlank() && customThemeName.isNotBlank()) {
                                            val safeName = customThemeName.trim().replace(":", " ").ifBlank { "Custom Combo" }
                                            val pArgb = android.graphics.Color.rgb((customColors.first.red * 255).toInt(), (customColors.first.green * 255).toInt(), (customColors.first.blue * 255).toInt())
                                            val sArgb = android.graphics.Color.rgb((customColors.second.red * 255).toInt(), (customColors.second.green * 255).toInt(), (customColors.second.blue * 255).toInt())
                                            val pHex = String.format("#%06X", pArgb and 0xFFFFFF)
                                            val sHex = String.format("#%06X", sArgb and 0xFFFFFF)
                                            onSaveCustomTheme("$safeName:$pHex|$sHex")
                                            customThemeName = ""
                                            customColorInput = ""
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Only")
                                }

                                // Apply & Save
                                Button(
                                    onClick = {
                                        if (customColorInput.isNotBlank()) {
                                            val pArgb = android.graphics.Color.rgb((customColors.first.red * 255).toInt(), (customColors.first.green * 255).toInt(), (customColors.first.blue * 255).toInt())
                                            val sArgb = android.graphics.Color.rgb((customColors.second.red * 255).toInt(), (customColors.second.green * 255).toInt(), (customColors.second.blue * 255).toInt())
                                            val pHex = String.format("#%06X", pArgb and 0xFFFFFF)
                                            val sHex = String.format("#%06X", sArgb and 0xFFFFFF)
                                            val comboStr = "CUSTOM:$pHex|$sHex"
                                            if (customThemeName.isNotBlank()) {
                                                val safeName = customThemeName.trim().replace(":", " ").ifBlank { "Custom Combo" }
                                                onSaveCustomTheme("$safeName:$pHex|$sHex")
                                            }
                                            selectedTemplate = comboStr
                                            if (onApplyThemeWithModes != null) {
                                                onApplyThemeWithModes(comboStr, localIsDark, localIsDynamic)
                                            } else {
                                                onDarkModeToggle(localIsDark)
                                                onDynamicColorToggle(localIsDynamic)
                                                onTemplateSelect(comboStr)
                                            }
                                        }
                                    },
                                    enabled = customColorInput.isNotBlank(),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isAdFreeActive) "Save & Apply" else "Save & Apply (2 Ads)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Your Saved Custom Combos (${savedCustomList.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (savedCustomList.isEmpty()) {
                        Text(
                            text = "No saved custom combos yet. Create one above!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            savedCustomList.forEach { item ->
                                val hasSeparator = item.contains(":")
                                val name = if (hasSeparator) item.substringBeforeLast(":") else "Custom Combo"
                                val combo = if (hasSeparator) item.substringAfterLast(":") else item
                                val subParts = combo.removePrefix("CUSTOM:").split("|")
                                val pColor = try { Color(android.graphics.Color.parseColor(subParts[0])) } catch (_: Exception) { Color.Magenta }
                                val sColor = try { Color(android.graphics.Color.parseColor(subParts.getOrNull(1) ?: subParts[0])) } catch (_: Exception) { Color.Cyan }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = BorderStroke(1.dp, if (selectedTemplate == "CUSTOM:$combo") MaterialTheme.colorScheme.primary else Color.Transparent)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(pColor))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(sColor))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(
                                                onClick = {
                                                    selectedTemplate = "CUSTOM:$combo"
                                                    if (onApplyThemeWithModes != null) {
                                                        onApplyThemeWithModes("CUSTOM:$combo", localIsDark, localIsDynamic)
                                                    } else {
                                                        onDarkModeToggle(localIsDark)
                                                        onDynamicColorToggle(localIsDynamic)
                                                        onTemplateSelect("CUSTOM:$combo")
                                                    }
                                                },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Text(
                                                    text = if (isAdFreeActive) "Apply" else "Apply (2 Ads)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteCustomTheme(item) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MaterialErrorColor(): Color {
    return MaterialTheme.colorScheme.error
}

data class ThemeOption(val id: String, val name: String, val primary: Color, val secondary: Color)

@Composable
fun ThemeCard(option: ThemeOption, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) option.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val bgColor = if (isSelected) option.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(option.primary))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(option.secondary))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = option.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(Icons.Default.Check, contentDescription = null, tint = option.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

fun parseCustomColors(input: String): Pair<Color, Color> {
    val lower = input.trim().lowercase()
    return when {
        lower.contains("sunset") || lower.contains("orange") || lower.contains("amber") -> Pair(Color(0xFFF97316), Color(0xFFFACC15))
        lower.contains("emerald") || lower.contains("green") || lower.contains("mint") || lower.contains("forest") -> Pair(Color(0xFF10B981), Color(0xFF34D399))
        lower.contains("ruby") || lower.contains("red") || lower.contains("crimson") || lower.contains("rose") -> Pair(Color(0xFFE11D48), Color(0xFFFB7185))
        lower.contains("cyber") || lower.contains("neon") || lower.contains("magenta") || lower.contains("pink") -> Pair(Color(0xFFFF007F), Color(0xFF00F0FF))
        lower.contains("ocean") || lower.contains("blue") || lower.contains("sky") || lower.contains("cyan") -> Pair(Color(0xFF0284C7), Color(0xFF38BDF8))
        lower.contains("purple") || lower.contains("violet") || lower.contains("indigo") || lower.contains("lavender") -> Pair(Color(0xFF8B5CF6), Color(0xFFEC4899))
        lower.contains("gold") || lower.contains("yellow") || lower.contains("obsidian") -> Pair(Color(0xFFFFD700), Color(0xFFD97706))
        lower.startsWith("#") || (lower.length >= 6 && lower.all { it in "0123456789abcdefABCDEF#" }) -> {
            try {
                val hexStr = if (lower.startsWith("#")) lower else "#$lower"
                val parsed = Color(android.graphics.Color.parseColor(hexStr))
                Pair(parsed, parsed.copy(red = (parsed.red + 0.2f).coerceAtMost(1f)))
            } catch (_: Exception) {
                Pair(Color(0xFF4F46E5), Color(0xFF06B6D4))
            }
        }
        else -> {
            if (lower.isNotBlank()) {
                val hash = lower.hashCode()
                val r = ((hash and 0xFF0000) shr 16).toFloat() / 255f
                val g = ((hash and 0x00FF00) shr 8).toFloat() / 255f
                val b = (hash and 0x0000FF).toFloat() / 255f
                val p = Color(r.coerceIn(0.2f, 0.9f), g.coerceIn(0.2f, 0.9f), b.coerceIn(0.2f, 0.9f))
                Pair(p, p.copy(red = (p.red + 0.25f).coerceAtMost(1f)))
            } else {
                Pair(Color(0xFF4F46E5), Color(0xFF06B6D4))
            }
        }
    }
}
