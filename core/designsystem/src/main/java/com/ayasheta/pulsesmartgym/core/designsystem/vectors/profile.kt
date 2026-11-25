package com.ayasheta.pulsesmartgym.core.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Profile: ImageVector
    get() {
        if (_Profile != null) {
            return _Profile!!
        }
        _Profile = ImageVector.Builder(
            name = "Profile",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 25f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF343C54)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(8.505f, 9.268f)
                curveTo(8.505f, 7.325f, 10.08f, 5.75f, 12.023f, 5.75f)
                curveTo(13.965f, 5.75f, 15.54f, 7.325f, 15.54f, 9.268f)
                curveTo(15.54f, 11.211f, 13.965f, 12.786f, 12.023f, 12.786f)
                curveTo(10.08f, 12.786f, 8.505f, 11.211f, 8.505f, 9.268f)
                close()
                moveTo(12.023f, 7.25f)
                curveTo(10.908f, 7.25f, 10.005f, 8.153f, 10.005f, 9.268f)
                curveTo(10.005f, 10.382f, 10.908f, 11.286f, 12.023f, 11.286f)
                curveTo(13.137f, 11.286f, 14.04f, 10.382f, 14.04f, 9.268f)
                curveTo(14.04f, 8.153f, 13.137f, 7.25f, 12.023f, 7.25f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF343C54)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(2.023f, 12f)
                curveTo(2.023f, 6.477f, 6.501f, 2f, 12.023f, 2f)
                curveTo(17.546f, 2f, 22.023f, 6.477f, 22.023f, 12f)
                curveTo(22.023f, 17.523f, 17.546f, 22f, 12.023f, 22f)
                curveTo(6.501f, 22f, 2.023f, 17.523f, 2.023f, 12f)
                close()
                moveTo(12.023f, 3.5f)
                curveTo(7.329f, 3.5f, 3.523f, 7.306f, 3.523f, 12f)
                curveTo(3.523f, 14.153f, 4.324f, 16.118f, 5.643f, 17.616f)
                curveTo(6.196f, 15.31f, 8.272f, 13.595f, 10.748f, 13.595f)
                horizontalLineTo(13.298f)
                curveTo(15.774f, 13.595f, 17.85f, 15.31f, 18.403f, 17.617f)
                curveTo(19.723f, 16.119f, 20.523f, 14.153f, 20.523f, 12f)
                curveTo(20.523f, 7.306f, 16.718f, 3.5f, 12.023f, 3.5f)
                close()
                moveTo(17.048f, 18.857f)
                verticalLineTo(18.846f)
                curveTo(17.048f, 16.774f, 15.369f, 15.095f, 13.298f, 15.095f)
                horizontalLineTo(10.748f)
                curveTo(8.677f, 15.095f, 6.998f, 16.774f, 6.998f, 18.846f)
                verticalLineTo(18.856f)
                curveTo(8.406f, 19.889f, 10.143f, 20.5f, 12.023f, 20.5f)
                curveTo(13.903f, 20.5f, 15.641f, 19.89f, 17.048f, 18.857f)
                close()
            }
        }.build()

        return _Profile!!
    }

@Suppress("ObjectPropertyName")
private var _Profile: ImageVector? = null

internal val ProfileFilled: ImageVector
    get() {
        if (_ProfileFilled != null) {
            return _ProfileFilled!!
        }
        _ProfileFilled = ImageVector.Builder(
            name = "ProfileFilled",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(11.999f, 5.75f)
                curveTo(10.056f, 5.75f, 8.481f, 7.325f, 8.481f, 9.268f)
                curveTo(8.481f, 11.211f, 10.056f, 12.786f, 11.999f, 12.786f)
                curveTo(13.942f, 12.786f, 15.517f, 11.211f, 15.517f, 9.268f)
                curveTo(15.517f, 7.325f, 13.942f, 5.75f, 11.999f, 5.75f)
                close()
            }
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                close()
                moveTo(3.5f, 12f)
                curveTo(3.5f, 7.306f, 7.306f, 3.5f, 12f, 3.5f)
                curveTo(16.694f, 3.5f, 20.5f, 7.306f, 20.5f, 12f)
                curveTo(20.5f, 14.153f, 19.7f, 16.119f, 18.38f, 17.617f)
                curveTo(17.827f, 15.31f, 15.751f, 13.595f, 13.275f, 13.595f)
                horizontalLineTo(10.725f)
                curveTo(8.249f, 13.595f, 6.173f, 15.31f, 5.619f, 17.616f)
                curveTo(4.3f, 16.118f, 3.5f, 14.153f, 3.5f, 12f)
                close()
            }
        }.build()

        return _ProfileFilled!!
    }

@Suppress("ObjectPropertyName")
private var _ProfileFilled: ImageVector? = null
