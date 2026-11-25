package com.ayasheta.pulsesmartgym.core.designsystem.semanticToken

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.R

val plusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

internal val defaultTypography = Typography(
    displayHero = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    numericMetric = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
    ),
    numericGauge = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 68.sp,
        lineHeight = 68.sp,
        letterSpacing = (-3.4).sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    sectionTitle = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    sectionHeader = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
    ),
    cardTitle = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    ),
    ctaPrimary = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    ),
    ctaCompact = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    subtitle = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    contentPrimary = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    contentSecondary = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    supportingContent = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
    caption = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    label = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    hint = TextStyle(
        fontFamily = plusJakartaSansFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
)
