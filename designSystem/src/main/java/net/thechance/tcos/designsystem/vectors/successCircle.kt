package net.thechance.tcos.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SuccessCircle: ImageVector
    get() {
        if (_SuccessCircle != null) {
            return _SuccessCircle!!
        }
        _SuccessCircle = ImageVector.Builder(
            name = "SuccessCircle",
            defaultWidth = 30.dp,
            defaultHeight = 30.dp,
            viewportWidth = 30f,
            viewportHeight = 30f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF85DF2A)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(27.5f, 15f)
                curveTo(27.5f, 21.904f, 21.904f, 27.5f, 15f, 27.5f)
                curveTo(8.096f, 27.5f, 2.5f, 21.904f, 2.5f, 15f)
                curveTo(2.5f, 8.096f, 8.096f, 2.5f, 15f, 2.5f)
                curveTo(21.904f, 2.5f, 27.5f, 8.096f, 27.5f, 15f)
                close()
                moveTo(20.038f, 11.212f)
                curveTo(20.404f, 11.578f, 20.404f, 12.172f, 20.038f, 12.538f)
                lineTo(13.788f, 18.788f)
                curveTo(13.422f, 19.154f, 12.828f, 19.154f, 12.462f, 18.788f)
                lineTo(9.962f, 16.288f)
                curveTo(9.596f, 15.922f, 9.596f, 15.328f, 9.962f, 14.962f)
                curveTo(10.328f, 14.596f, 10.922f, 14.596f, 11.288f, 14.962f)
                lineTo(13.125f, 16.799f)
                lineTo(15.918f, 14.006f)
                lineTo(18.712f, 11.212f)
                curveTo(19.078f, 10.846f, 19.672f, 10.846f, 20.038f, 11.212f)
                close()
            }
        }.build()

        return _SuccessCircle!!
    }

@Suppress("ObjectPropertyName")
private var _SuccessCircle: ImageVector? = null
