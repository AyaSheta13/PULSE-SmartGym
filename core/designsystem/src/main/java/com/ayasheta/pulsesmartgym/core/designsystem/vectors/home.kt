package com.ayasheta.pulsesmartgym.core.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Home: ImageVector
    get() {
        if (_Home != null) {
            return _Home!!
        }
        _Home = ImageVector.Builder(
            name = "Home",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(12.488f, 4.106f)
                lineTo(20.488f, 10.957f)
                lineTo(20.544f, 11.011f)
                curveTo(20.806f, 11.286f, 20.822f, 11.72f, 20.569f, 12.015f)
                curveTo(20.317f, 12.31f, 19.885f, 12.36f, 19.573f, 12.144f)
                lineTo(19.512f, 12.097f)
                lineTo(18.75f, 11.443f)
                verticalLineTo(19.25f)
                horizontalLineTo(20f)
                curveTo(20.414f, 19.25f, 20.75f, 19.586f, 20.75f, 20f)
                curveTo(20.75f, 20.414f, 20.414f, 20.75f, 20f, 20.75f)
                horizontalLineTo(4f)
                curveTo(3.586f, 20.75f, 3.25f, 20.414f, 3.25f, 20f)
                curveTo(3.25f, 19.586f, 3.586f, 19.25f, 4f, 19.25f)
                horizontalLineTo(5.25f)
                verticalLineTo(11.443f)
                lineTo(4.488f, 12.097f)
                lineTo(4.427f, 12.144f)
                curveTo(4.115f, 12.36f, 3.683f, 12.31f, 3.431f, 12.015f)
                curveTo(3.161f, 11.7f, 3.197f, 11.227f, 3.512f, 10.957f)
                lineTo(11.512f, 4.106f)
                curveTo(11.793f, 3.865f, 12.207f, 3.865f, 12.488f, 4.106f)
                close()
                moveTo(12.488f, 6.081f)
                curveTo(12.243f, 5.871f, 11.894f, 5.844f, 11.623f, 6.002f)
                lineTo(11.512f, 6.081f)
                lineTo(6.75f, 10.159f)
                verticalLineTo(19.25f)
                horizontalLineTo(17.25f)
                verticalLineTo(10.159f)
                lineTo(12.488f, 6.081f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF343C54)),
                fillAlpha = 0.4f,
                strokeAlpha = 0.4f
            ) {
                moveTo(12f, 13.08f)
                curveTo(13.381f, 13.08f, 14.5f, 14.199f, 14.5f, 15.58f)
                verticalLineTo(19.25f)
                horizontalLineTo(13f)
                verticalLineTo(15.58f)
                curveTo(13f, 15.028f, 12.552f, 14.58f, 12f, 14.58f)
                curveTo(11.448f, 14.58f, 11f, 15.028f, 11f, 15.58f)
                verticalLineTo(19.25f)
                horizontalLineTo(9.5f)
                verticalLineTo(15.58f)
                curveTo(9.5f, 14.199f, 10.619f, 13.08f, 12f, 13.08f)
                close()
            }
        }.build()

        return _Home!!
    }

@Suppress("ObjectPropertyName")
private var _Home: ImageVector? = null

internal val HomeFilled: ImageVector
    get() {
        if (_HomeFilled != null) {
            return _HomeFilled!!
        }
        _HomeFilled = ImageVector.Builder(
            name = "HomeFilled",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 25f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(12.012f, 4.106f)
                lineTo(4.012f, 10.958f)
                curveTo(3.698f, 11.227f, 3.661f, 11.7f, 3.93f, 12.015f)
                curveTo(4.2f, 12.33f, 4.673f, 12.366f, 4.988f, 12.097f)
                lineTo(5.75f, 11.444f)
                verticalLineTo(19.25f)
                horizontalLineTo(4.5f)
                curveTo(4.086f, 19.25f, 3.75f, 19.586f, 3.75f, 20f)
                curveTo(3.75f, 20.414f, 4.086f, 20.75f, 4.5f, 20.75f)
                horizontalLineTo(20.5f)
                curveTo(20.914f, 20.75f, 21.25f, 20.414f, 21.25f, 20f)
                curveTo(21.25f, 19.586f, 20.914f, 19.25f, 20.5f, 19.25f)
                horizontalLineTo(19.25f)
                verticalLineTo(11.444f)
                lineTo(20.012f, 12.097f)
                curveTo(20.327f, 12.366f, 20.8f, 12.33f, 21.07f, 12.015f)
                curveTo(21.339f, 11.7f, 21.302f, 11.227f, 20.988f, 10.958f)
                lineTo(12.988f, 4.106f)
                curveTo(12.707f, 3.865f, 12.293f, 3.865f, 12.012f, 4.106f)
                close()
                moveTo(14.625f, 15.955f)
                verticalLineTo(19.25f)
                horizontalLineTo(10.375f)
                verticalLineTo(15.955f)
                curveTo(10.375f, 14.781f, 11.326f, 13.83f, 12.5f, 13.83f)
                curveTo(13.674f, 13.83f, 14.625f, 14.781f, 14.625f, 15.955f)
                close()
            }
        }.build()

        return _HomeFilled!!
    }

@Suppress("ObjectPropertyName")
private var _HomeFilled: ImageVector? = null