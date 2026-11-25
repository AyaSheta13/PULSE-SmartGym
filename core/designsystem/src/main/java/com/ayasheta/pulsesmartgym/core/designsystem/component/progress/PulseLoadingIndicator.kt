package com.ayasheta.pulsesmartgym.core.designsystem.component.progress

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

@Composable
fun PulseLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.5.dp,
    color: Color = PulseTheme.color.primary,
    trackColor: Color = PulseTheme.color.surfaceMuted,
) {
    val transition = rememberInfiniteTransition(label = "pulse_loading")
    val startAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_loading_angle",
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = 270f,
            useCenter = false,
            style = stroke,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseLoadingIndicatorPreview() {
    PulseTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            PulseLoadingIndicator()
        }
    }
}
