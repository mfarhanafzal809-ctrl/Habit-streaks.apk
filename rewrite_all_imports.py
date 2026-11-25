with open('app/src/main/java/com/example/ui/components/HabitCard.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
in_imports = True
for line in lines:
    if line.startswith('import '):
        pass # Skip existing imports
    elif line.strip() == '' and in_imports:
        pass
    else:
        if in_imports and line.startswith('@Composable'):
            in_imports = False
        if not in_imports:
            new_lines.append(line)

imports = """package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitWithStats
import com.example.data.model.RingMode
import com.example.ui.theme.HabitAmber
import com.example.ui.theme.PrimaryIndigo
import java.util.Locale

"""

with open('app/src/main/java/com/example/ui/components/HabitCard.kt', 'w') as f:
    f.write(imports + "".join(new_lines))
