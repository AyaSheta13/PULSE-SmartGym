package net.thechance.tcos.designsystem.semanticToken

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.thechance.tcos.designsystem.R

private val rubikFontFamily = FontFamily(
    Font(R.font.rubik_medium, FontWeight.Medium),
    Font(R.font.rubik_regular, FontWeight.Normal)
)

internal val defaultTypography = Typography(
    sectionTitle = TextStyle(
        fontFamily = rubikFontFamily,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    ),
    cardTitle = TextStyle(
        fontFamily = rubikFontFamily,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium
    ),
    subtitle = TextStyle(
        fontFamily = rubikFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    contentPrimary = TextStyle(
        fontFamily = rubikFontFamily,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    ),
    contentSecondary = TextStyle(
        fontFamily = rubikFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    supportingContent = TextStyle(
        fontFamily = rubikFontFamily,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    ),
    caption = TextStyle(
        fontFamily = rubikFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    label = TextStyle(
        fontFamily = rubikFontFamily,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium
    ),
    hint = TextStyle(
        fontFamily = rubikFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
)