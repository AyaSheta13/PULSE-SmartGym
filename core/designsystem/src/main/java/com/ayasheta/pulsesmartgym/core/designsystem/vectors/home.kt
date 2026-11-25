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
                moveTo(13.237f, 3.47f)
                curveTo(13.53f, 3.177f, 14.005f, 3.177f, 14.298f, 3.47f)
                curveTo(14.591f, 3.763f, 14.591f, 4.238f, 14.298f, 4.53f)
                lineTo(13.691f, 5.136f)
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
                lineTo(10.308f, 5.136f)
                lineTo(9.702f, 4.53f)
                lineTo(9.65f, 4.474f)
                curveTo(9.41f, 4.179f, 9.428f, 3.744f, 9.702f, 3.47f)
                curveTo(9.977f, 3.195f, 10.41f, 3.178f, 10.705f, 3.418f)
                lineTo(10.763f, 3.47f)
                lineTo(11.47f, 4.177f)
                curveTo(11.763f, 4.47f, 12.237f, 4.47f, 12.53f, 4.177f)
                lineTo(13.237f, 3.47f)
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
                moveTo(11.262f, 3.47f)
                curveTo(10.97f, 3.177f, 10.495f, 3.177f, 10.202f, 3.47f)
                curveTo(9.909f, 3.763f, 9.909f, 4.237f, 10.202f, 4.53f)
                lineTo(10.808f, 5.137f)
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
                lineTo(14.192f, 5.137f)
                lineTo(14.798f, 4.53f)
                curveTo(15.091f, 4.237f, 15.091f, 3.763f, 14.798f, 3.47f)
                curveTo(14.505f, 3.177f, 14.031f, 3.177f, 13.738f, 3.47f)
                lineTo(13.03f, 4.177f)
                curveTo(12.738f, 4.47f, 12.263f, 4.47f, 11.97f, 4.177f)
                lineTo(11.262f, 3.47f)
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