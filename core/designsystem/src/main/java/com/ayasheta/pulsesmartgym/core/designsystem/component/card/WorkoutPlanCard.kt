package com.ayasheta.pulsesmartgym.core.designsystem.component.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Play
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Workouts

@Composable
fun PulseWorkoutPlanCard(
    title: String,
    metadata: String,
    badgeLabel: String,
    ctaLabel: String,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: Painter? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = PulseTheme.corners.large,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(208.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3A3533),
                            Color(0xFF1C1B1B),
                        ),
                    ),
                ),
        ) {
            image?.let {
                Image(
                    painter = it,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .padding(PulseTheme.space.medium)
                    .clip(PulseTheme.corners.fullRounded)
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(PulseTheme.corners.fullRounded)
                        .background(PulseTheme.color.primary),
                )
                BasicText(
                    text = badgeLabel.uppercase(),
                    style = PulseTheme.typography.label.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.28.sp,
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(PulseTheme.space.medium),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(
                    text = title,
                    style = PulseTheme.typography.displayHero.copy(
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        color = PulseTheme.color.onPrimary,
                    ),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Image(
                        imageVector = Workouts,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0xFFE6F3F0EF)),
                        modifier = Modifier.size(14.dp),
                    )
                    BasicText(
                        text = metadata,
                        style = PulseTheme.typography.contentSecondary.copy(
                            color = Color(0xFFE6F3F0EF),
                        ),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PulseTheme.color.container)
                .padding(PulseTheme.space.large),
        ) {
            Button(
                caption = ctaLabel,
                icon = rememberVectorPainter(Play),
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseWorkoutPlanCardPreview() {
    PulseTheme {
        Box(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(16.dp),
        ) {
            PulseWorkoutPlanCard(
                title = "Upper Body Power",
                metadata = "4 exercises • 45 min • 16 planned sets",
                badgeLabel = "TODAY'S WORKOUT • POWER & STRENGTH",
                ctaLabel = "Start Workout",
                onStartClick = {},
            )
        }
    }
}
