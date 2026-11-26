package com.ayasheta.pulsesmartgym.feature.device.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun PulsePodIllustration(
    label: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isConnected) Color(0xFF00B87C) else Color(0xFF343842)
    val ringBorder = if (isConnected) Color(0xFF00B87C) else Color(0xFF6B7280)
    val ringFill = if (isConnected) Color(0xFFD7F7EA) else Color.Transparent

    Box(
        modifier = modifier
            .size(116.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1F232B))
            .border(BorderStroke(2.5.dp, borderColor), RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ringFill)
                    .border(BorderStroke(1.8.dp, ringBorder), CircleShape),
            )
            BasicText(
                text = label,
                style = PulseTheme.typography.label.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isConnected) Color.White else Color(0xFFA0A6B1),
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
fun BluetoothRadarIndicator(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulsePhase = if (isAnimating) {
        val transition = rememberInfiniteTransition(label = "bt_radar")
        val anim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "bt_radar_phase",
        )
        anim
    } else {
        0f
    }

    Box(
        modifier = modifier.size(216.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(216.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val minRadius = 28.dp.toPx()
            val maxRadius = size.minDimension / 2f
            if (isAnimating) {
                val ringOffsets = floatArrayOf(0f, 0.33f, 0.66f)
                ringOffsets.forEach { offset ->
                    val progress = (pulsePhase + offset) % 1f
                    val radius = minRadius + (maxRadius - minRadius) * progress
                    val alpha = (1f - progress) * 0.65f
                    drawCircle(
                        color = WorkoutPalette.PrimaryCoral.copy(alpha = alpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            } else {
                drawCircle(
                    color = Color(0xFFFFD8CE),
                    radius = maxRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawCircle(
                    color = Color(0xFFFFB4A1),
                    radius = size.minDimension * 0.34f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawCircle(
                    color = Color(0xFFFF886C),
                    radius = size.minDimension * 0.21f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(WorkoutPalette.PrimaryCoral),
            contentAlignment = Alignment.Center,
        ) {
            BluetoothVectorIcon(
                color = Color.White,
                size = 24.dp,
            )
        }
    }
}

@Composable
fun BluetoothVectorIcon(
    color: Color,
    size: Dp = 20.dp,
    crossedOut: Boolean = false,
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.28f)
            lineTo(w * 0.70f, h * 0.65f)
            lineTo(w * 0.50f, h * 0.85f)
            lineTo(w * 0.50f, h * 0.15f)
            lineTo(w * 0.70f, h * 0.35f)
            lineTo(w * 0.35f, h * 0.72f)
        }
        drawPath(path = path, color = color, style = stroke)
        if (crossedOut) {
            drawLine(
                color = WorkoutPalette.PrimaryCoral,
                start = Offset(w * 0.16f, h * 0.16f),
                end = Offset(w * 0.84f, h * 0.84f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
