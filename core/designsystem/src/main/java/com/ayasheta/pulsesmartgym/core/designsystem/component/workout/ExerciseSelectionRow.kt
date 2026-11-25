package com.ayasheta.pulsesmartgym.core.designsystem.component.workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.component.card.Card
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Plus
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Workouts

@Composable
fun PulseExerciseSelectionRow(
    name: String,
    muscleGroup: String,
    equipment: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: Painter? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = PulseTheme.corners.xLarge,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(PulseTheme.corners.large)
                        .background(PulseTheme.color.surfaceMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    if (thumbnail != null) {
                        Image(
                            painter = thumbnail,
                            contentDescription = name,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Image(
                            imageVector = Workouts,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(PulseTheme.color.shadeSecondary),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        text = name,
                        style = PulseTheme.typography.cardTitle.copy(
                            fontWeight = FontWeight.Medium,
                            color = PulseTheme.color.shadePrimary,
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(PulseTheme.corners.fullRounded)
                                .background(PulseTheme.color.surfaceMuted)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            BasicText(
                                text = muscleGroup,
                                style = PulseTheme.typography.label.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.color.shadeSecondary,
                                ),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(PulseTheme.corners.fullRounded)
                                .background(PulseTheme.color.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            BasicText(
                                text = equipment.uppercase(),
                                style = PulseTheme.typography.label.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.color.primaryDark,
                                ),
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(44.dp)
                    .clip(PulseTheme.corners.large)
                    .background(if (isSelected) PulseTheme.color.primary else PulseTheme.color.surfaceMuted)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else PulseTheme.color.border,
                        shape = PulseTheme.corners.large,
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = if (isSelected) CheckMark else Plus,
                    contentDescription = if (isSelected) "Selected" else "Select exercise",
                    colorFilter = ColorFilter.tint(
                        if (isSelected) PulseTheme.color.onPrimary else PulseTheme.color.shadeSecondary,
                    ),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseExerciseSelectionRowPreview() {
    PulseTheme {
        Column(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PulseExerciseSelectionRow(
                name = "Cable Row",
                muscleGroup = "Back • Lats",
                equipment = "Cables",
                isSelected = true,
                onToggle = {},
            )
            PulseExerciseSelectionRow(
                name = "Barbell Bench Press",
                muscleGroup = "Chest • Triceps",
                equipment = "Barbell",
                isSelected = false,
                onToggle = {},
            )
        }
    }
}
