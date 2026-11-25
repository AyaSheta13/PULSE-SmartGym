package com.ayasheta.pulsesmartgym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ayasheta.pulsesmartgym.core.designsystem.component.ScreenSkeleton
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.SectionTitle
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseTheme {
                PulseFoundationPlaceholder()
            }
        }
    }
}

@Composable
private fun PulseFoundationPlaceholder(modifier: Modifier = Modifier) {
    ScreenSkeleton(
        modifier = modifier,
        topBar = {
            PulseTopAppBar(
                style = PulseTopAppBarStyle.TitleOnly(title = "PULSE Smart Gym"),
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(PulseTheme.space.large),
            contentAlignment = Alignment.Center,
        ) {
            SectionTitle(text = "PULSE Design System Ready")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseFoundationPlaceholderPreview() {
    PulseTheme {
        PulseFoundationPlaceholder()
    }
}
