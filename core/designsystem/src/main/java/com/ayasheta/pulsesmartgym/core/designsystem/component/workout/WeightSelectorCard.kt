package com.ayasheta.pulsesmartgym.core.designsystem.component.workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.component.card.Card
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Minus
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Plus

@Composable
fun PulseWeightSelectorCard(
    weightText: String,
    unitLabel: String,
    setSubtitle: String,
    targetText: String,
    onDecrementClick: () -> Unit,
    onIncrementClick: () -> Unit,
    modifier: Modifier = Modifier,
    headerLabel: String = "CURRENT SET LOAD",
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = PulseTheme.corners.xLarge,
        contentPadding = PaddingValues(horizontal = PulseTheme.space.large, vertical = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = headerLabel,
                    style = PulseTheme.typography.label.copy(
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
                Box(
                    modifier = Modifier
                        .clip(PulseTheme.corners.small)
                        .background(PulseTheme.color.background)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    BasicText(
                        text = targetText,
                        style = PulseTheme.typography.label.copy(
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.color.shadePrimary,
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        BasicText(
                            text = weightText,
                            style = PulseTheme.typography.numericMetric.copy(
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                                color = PulseTheme.color.shadePrimary,
                            ),
                        )
                        BasicText(
                            text = unitLabel,
                            style = PulseTheme.typography.sectionHeader.copy(
                                color = PulseTheme.color.shadePrimary,
                            ),
                        )
                    }
                    BasicText(
                        text = setSubtitle,
                        style = PulseTheme.typography.label.copy(
                            fontWeight = FontWeight.Medium,
                            color = PulseTheme.color.shadeSecondary,
                        ),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WeightStepButton(
                        icon = Minus,
                        contentDescription = "Decrease weight",
                        onClick = onDecrementClick,
                    )
                    WeightStepButton(
                        icon = Plus,
                        contentDescription = "Increase weight",
                        onClick = onIncrementClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightStepButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(PulseTheme.corners.medium)
            .background(PulseTheme.color.surfaceMuted)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(PulseTheme.color.shadePrimary),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseWeightSelectorCardPreview() {
    PulseTheme {
        Box(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(16.dp),
        ) {
            PulseWeightSelectorCard(
                weightText = "80",
                unitLabel = "kg",
                setSubtitle = "Actual load used for Set 2",
                targetText = "Target: 80 kg",
                onDecrementClick = {},
                onIncrementClick = {},
            )
        }
    }
}
