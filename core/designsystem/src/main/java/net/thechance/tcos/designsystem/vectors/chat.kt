package net.thechance.tcos.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Chat: ImageVector
    get() {
        if (_Chat != null) {
            return _Chat!!
        }
        _Chat = ImageVector.Builder(
            name = "Chat",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF343C54)),
                fillAlpha = 0.4f,
                strokeAlpha = 0.4f
            ) {
                moveTo(2f, 11.25f)
                curveTo(2f, 6.672f, 6.658f, 3.25f, 12f, 3.25f)
                curveTo(17.342f, 3.25f, 22f, 6.672f, 22f, 11.25f)
                curveTo(22f, 15.633f, 17.724f, 18.959f, 12.683f, 19.232f)
                lineTo(7.584f, 21.926f)
                curveTo(7.351f, 22.049f, 7.071f, 22.041f, 6.846f, 21.905f)
                curveTo(6.621f, 21.77f, 6.483f, 21.526f, 6.483f, 21.263f)
                verticalLineTo(17.928f)
                curveTo(3.842f, 16.53f, 2f, 14.104f, 2f, 11.25f)
                close()
                moveTo(12f, 4.75f)
                curveTo(7.125f, 4.75f, 3.5f, 7.82f, 3.5f, 11.25f)
                curveTo(3.5f, 13.519f, 5.05f, 15.605f, 7.554f, 16.787f)
                curveTo(7.816f, 16.911f, 7.983f, 17.175f, 7.983f, 17.465f)
                verticalLineTo(20.018f)
                lineTo(12.13f, 17.828f)
                curveTo(12.229f, 17.775f, 12.339f, 17.746f, 12.451f, 17.742f)
                curveTo(17.105f, 17.555f, 20.5f, 14.573f, 20.5f, 11.25f)
                curveTo(20.5f, 7.82f, 16.875f, 4.75f, 12f, 4.75f)
                close()
            }
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(7f, 9.75f)
                curveTo(7f, 9.336f, 7.336f, 9f, 7.75f, 9f)
                horizontalLineTo(16.25f)
                curveTo(16.664f, 9f, 17f, 9.336f, 17f, 9.75f)
                curveTo(17f, 10.164f, 16.664f, 10.5f, 16.25f, 10.5f)
                horizontalLineTo(7.75f)
                curveTo(7.336f, 10.5f, 7f, 10.164f, 7f, 9.75f)
                close()
            }
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(7.75f, 12f)
                curveTo(7.336f, 12f, 7f, 12.336f, 7f, 12.75f)
                curveTo(7f, 13.164f, 7.336f, 13.5f, 7.75f, 13.5f)
                horizontalLineTo(12.75f)
                curveTo(13.164f, 13.5f, 13.5f, 13.164f, 13.5f, 12.75f)
                curveTo(13.5f, 12.336f, 13.164f, 12f, 12.75f, 12f)
                horizontalLineTo(7.75f)
                close()
            }
        }.build()

        return _Chat!!
    }

@Suppress("ObjectPropertyName")
private var _Chat: ImageVector? = null


internal val ChatFilled: ImageVector
    get() {
        if (_ChatFilled != null) {
            return _ChatFilled!!
        }
        _ChatFilled = ImageVector.Builder(
            name = "ChatFilled",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(2f, 11.25f)
                curveTo(2f, 6.672f, 6.658f, 3.25f, 12f, 3.25f)
                curveTo(17.342f, 3.25f, 22f, 6.672f, 22f, 11.25f)
                curveTo(22f, 15.633f, 17.724f, 18.959f, 12.683f, 19.232f)
                lineTo(7.584f, 21.926f)
                curveTo(7.351f, 22.049f, 7.071f, 22.041f, 6.846f, 21.905f)
                curveTo(6.621f, 21.77f, 6.483f, 21.526f, 6.483f, 21.263f)
                verticalLineTo(17.928f)
                curveTo(3.842f, 16.53f, 2f, 14.104f, 2f, 11.25f)
                close()
                moveTo(7.75f, 9.847f)
                curveTo(7.336f, 9.847f, 7f, 10.182f, 7f, 10.597f)
                curveTo(7f, 11.011f, 7.336f, 11.347f, 7.75f, 11.347f)
                horizontalLineTo(16.25f)
                curveTo(16.664f, 11.347f, 17f, 11.011f, 17f, 10.597f)
                curveTo(17f, 10.182f, 16.664f, 9.847f, 16.25f, 9.847f)
                horizontalLineTo(7.75f)
                close()
                moveTo(7f, 13.597f)
                curveTo(7f, 14.011f, 7.336f, 14.347f, 7.75f, 14.347f)
                horizontalLineTo(12.75f)
                curveTo(13.164f, 14.347f, 13.5f, 14.011f, 13.5f, 13.597f)
                curveTo(13.5f, 13.182f, 13.164f, 12.847f, 12.75f, 12.847f)
                horizontalLineTo(7.75f)
                curveTo(7.336f, 12.847f, 7f, 13.182f, 7f, 13.597f)
                close()
            }
        }.build()

        return _ChatFilled!!
    }

@Suppress("ObjectPropertyName")
private var _ChatFilled: ImageVector? = null