package com.ayasheta.pulsesmartgym.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthColors
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onResolved: (SplashDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resolveStartupDestination()
    }

    LaunchedEffect(uiState.destination) {
        val target = uiState.destination ?: return@LaunchedEffect
        onResolved(target)
    }

    SplashContent(modifier = modifier)
}

@Composable
fun SplashContent(
    modifier: Modifier = Modifier,
) {
    var hasPlayedEntrance by rememberSaveable { mutableStateOf(false) }
    val contentAlpha = remember { Animatable(if (hasPlayedEntrance) 1f else 0f) }
    val logoScale = remember { Animatable(if (hasPlayedEntrance) 1f else 0.90f) }

    LaunchedEffect(Unit) {
        if (!hasPlayedEntrance) {
            hasPlayedEntrance = true
            launch {
                contentAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                )
            }
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseAuthColors.ScreenSurface),
    ) {
        // Top ambient coral radial glow matching Figma 57:20481
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-128).dp)
                .size(384.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1AFF623E),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // Bottom ambient sky radial glow matching Figma 57:20482
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 96.dp)
                .size(320.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1A39B8FD),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    alpha = contentAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Ambient aura + Tactile Squircle Icon Enclosure (Figma 57:20506..57:20508)
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x26FF623E),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color(0x2E1C1B1B),
                            spotColor = Color(0x2E1C1B1B),
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(PulseTheme.color.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_pulse_bolt_splash),
                        contentDescription = "PULSE logo",
                        modifier = Modifier.size(width = 50.dp, height = 56.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Wordmark: PULSE + 10dp coral dot (Figma 57:20498..57:20502)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = "PULSE",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.displayHero.fontFamily,
                        fontSize = 32.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp,
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 7.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(PulseTheme.color.primary),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            BasicText(
                text = "SMART GYM",
                style = TextStyle(
                    fontFamily = PulseTheme.typography.displayHero.fontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = Color(0xFF5A413B),
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun SplashPreview() {
    PulseTheme {
        SplashContent()
    }
}
