package net.thechance.tcos.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Notification: ImageVector
    get() {
        if (_Notification != null) {
            return _Notification!!
        }
        _Notification = ImageVector.Builder(
            name = "Notification",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF343C54)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(12.75f, 2.751f)
                curveTo(12.75f, 2.337f, 12.414f, 2.001f, 12f, 2.001f)
                curveTo(11.586f, 2.001f, 11.25f, 2.337f, 11.25f, 2.751f)
                verticalLineTo(3.538f)
                curveTo(7.46f, 3.914f, 4.5f, 7.112f, 4.5f, 11.001f)
                verticalLineTo(17.501f)
                horizontalLineTo(4f)
                curveTo(3.586f, 17.501f, 3.25f, 17.837f, 3.25f, 18.251f)
                curveTo(3.25f, 18.665f, 3.586f, 19.001f, 4f, 19.001f)
                horizontalLineTo(20f)
                curveTo(20.414f, 19.001f, 20.75f, 18.665f, 20.75f, 18.251f)
                curveTo(20.75f, 17.837f, 20.414f, 17.501f, 20f, 17.501f)
                horizontalLineTo(19.5f)
                verticalLineTo(11.001f)
                curveTo(19.5f, 7.112f, 16.54f, 3.914f, 12.75f, 3.538f)
                verticalLineTo(2.751f)
                close()
                moveTo(18f, 17.501f)
                verticalLineTo(11.001f)
                curveTo(18f, 7.687f, 15.314f, 5.001f, 12f, 5.001f)
                curveTo(8.686f, 5.001f, 6f, 7.687f, 6f, 11.001f)
                verticalLineTo(17.501f)
                horizontalLineTo(18f)
                close()
            }
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(10.5f, 20.5f)
                curveTo(10.086f, 20.5f, 9.75f, 20.836f, 9.75f, 21.25f)
                curveTo(9.75f, 21.664f, 10.086f, 22f, 10.5f, 22f)
                horizontalLineTo(13.5f)
                curveTo(13.914f, 22f, 14.25f, 21.664f, 14.25f, 21.25f)
                curveTo(14.25f, 20.836f, 13.914f, 20.5f, 13.5f, 20.5f)
                horizontalLineTo(10.5f)
                close()
            }
        }.build()

        return _Notification!!
    }

@Suppress("ObjectPropertyName")
private var _Notification: ImageVector? = null
