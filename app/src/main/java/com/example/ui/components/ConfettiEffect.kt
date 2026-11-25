package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var width: Float,
    var height: Float,
    var color: Color,
    var rotation: Float,
    var rotationSpeed: Float,
    var shape: Int, // 0 = rect ribbon, 1 = circle, 2 = diamond
    var opacity: Float = 1f
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    triggerKey: Any = Unit
) {
    var particles by remember(triggerKey) { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    val progress = remember(triggerKey) { Animatable(0f) }

    val colors = remember {
        listOf(
            Color(0xFFFFD700), // Gold
            Color(0xFFFF3366), // Rose
            Color(0xFF00E5FF), // Cyan
            Color(0xFF76FF03), // Lime
            Color(0xFF8B5CF6), // Purple
            Color(0xFFFF9100), // Amber
            Color(0xFFEC4899), // Pink
            Color(0xFF38BDF8), // Sky Blue
            Color(0xFFFFFFFF)  // Sparkle White
        )
    }

    LaunchedEffect(triggerKey) {
        // Generate burst of confetti
        val random = Random(System.currentTimeMillis())
        val initialParticles = (0..85).map {
            ConfettiParticle(
                x = 0.5f + (random.nextFloat() - 0.5f) * 0.4f,
                y = 0.7f + random.nextFloat() * 0.2f,
                vx = (random.nextFloat() - 0.5f) * 1.8f,
                vy = - (1.2f + random.nextFloat() * 2.2f),
                width = 8f + random.nextFloat() * 12f,
                height = 6f + random.nextFloat() * 14f,
                color = colors[random.nextInt(colors.size)],
                rotation = random.nextFloat() * 360f,
                rotationSpeed = (random.nextFloat() - 0.5f) * 20f,
                shape = random.nextInt(3),
                opacity = 1f
            )
        }
        particles = initialParticles

        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3800, easing = LinearEasing)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val p = progress.value
        if (p < 1f && particles.isNotEmpty()) {
            particles.forEach { particle ->
                // Physics update
                val currentX = (particle.x + particle.vx * p * 0.9f) * canvasWidth
                val currentY = (particle.y + particle.vy * p * 0.8f + (p * p * 2.4f)) * canvasHeight
                val currentRotation = particle.rotation + particle.rotationSpeed * p * 30f
                val alpha = (1f - (p * 1.1f).coerceIn(0f, 1f)).coerceIn(0f, 1f)
                val drawColor = particle.color.copy(alpha = alpha)

                if (currentY in -50f..canvasHeight + 50f && currentX in -50f..canvasWidth + 50f) {
                    rotate(degrees = currentRotation, pivot = Offset(currentX, currentY)) {
                        when (particle.shape) {
                            0 -> { // Ribbon
                                drawRect(
                                    color = drawColor,
                                    topLeft = Offset(currentX - particle.width / 2, currentY - particle.height / 2),
                                    size = Size(particle.width, particle.height)
                                )
                            }
                            1 -> { // Circle Dot
                                drawCircle(
                                    color = drawColor,
                                    radius = particle.width / 2,
                                    center = Offset(currentX, currentY)
                                )
                            }
                            else -> { // Diamond
                                val halfW = particle.width * 0.6f
                                val halfH = particle.height * 0.6f
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(currentX, currentY - halfH)
                                    lineTo(currentX + halfW, currentY)
                                    lineTo(currentX, currentY + halfH)
                                    lineTo(currentX - halfW, currentY)
                                    close()
                                }
                                drawPath(path = path, color = drawColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
