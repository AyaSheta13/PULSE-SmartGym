package net.thechance.tcos.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ErrorCircle: ImageVector
    get() {
        if (_ErrorCircle != null) {
            return _ErrorCircle!!
        }
        _ErrorCircle = ImageVector.Builder(
            name = "ErrorCircle",
            defaultWidth = 30.dp,
            defaultHeight = 30.dp,
            viewportWidth = 30f,
            viewportHeight = 30f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFDF2A2A)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(27.5f, 15f)
                curveTo(27.5f, 21.904f, 21.904f, 27.5f, 15f, 27.5f)
                curveTo(8.096f, 27.5f, 2.5f, 21.904f, 2.5f, 15f)
                curveTo(2.5f, 8.096f, 8.096f, 2.5f, 15f, 2.5f)
                curveTo(21.904f, 2.5f, 27.5f, 8.096f, 27.5f, 15f)
                close()
                moveTo(11.212f, 11.212f)
                curveTo(11.578f, 10.846f, 12.172f, 10.846f, 12.538f, 11.212f)
                lineTo(15f, 13.674f)
                lineTo(17.462f, 11.212f)
                curveTo(17.828f, 10.846f, 18.422f, 10.846f, 18.788f, 11.212f)
                curveTo(19.154f, 11.578f, 19.154f, 12.172f, 18.788f, 12.538f)
                lineTo(16.326f, 15f)
                lineTo(18.788f, 17.462f)
                curveTo(19.154f, 17.828f, 19.154f, 18.422f, 18.788f, 18.788f)
                curveTo(18.422f, 19.154f, 17.828f, 19.154f, 17.462f, 18.788f)
                lineTo(15f, 16.326f)
                lineTo(12.538f, 18.788f)
                curveTo(12.172f, 19.154f, 11.578f, 19.154f, 11.212f, 18.788f)
                curveTo(10.846f, 18.422f, 10.846f, 17.828f, 11.212f, 17.462f)
                lineTo(13.674f, 15f)
                lineTo(11.212f, 12.538f)
                curveTo(10.846f, 12.172f, 10.846f, 11.578f, 11.212f, 11.212f)
                close()
            }
        }.build()

        return _ErrorCircle!!
    }

@Suppress("ObjectPropertyName")
private var _ErrorCircle: ImageVector? = null
