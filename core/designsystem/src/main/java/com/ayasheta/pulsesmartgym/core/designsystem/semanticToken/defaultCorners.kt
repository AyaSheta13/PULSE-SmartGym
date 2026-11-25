package com.ayasheta.pulsesmartgym.core.designsystem.semanticToken

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

val defaultCorners = Corners(
    notRounded = RectangleShape,
    xSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    xLarge = RoundedCornerShape(16.dp),
    xxLarge = RoundedCornerShape(24.dp),
    fullRounded = RoundedCornerShape(percent = 50),
)
