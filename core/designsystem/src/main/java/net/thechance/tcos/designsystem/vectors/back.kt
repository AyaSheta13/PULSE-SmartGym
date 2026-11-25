package net.thechance.tcos.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Back: ImageVector
    get() {
        if (_Back != null) {
            return _Back!!
        }
        _Back = ImageVector.Builder(
            name = "Back",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 25f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(15.233f, 5.22f)
                curveTo(15.526f, 5.513f, 15.526f, 5.987f, 15.233f, 6.28f)
                lineTo(9.514f, 12f)
                lineTo(15.233f, 17.72f)
                curveTo(15.526f, 18.013f, 15.526f, 18.487f, 15.233f, 18.78f)
                curveTo(14.941f, 19.073f, 14.466f, 19.073f, 14.173f, 18.78f)
                lineTo(7.923f, 12.53f)
                curveTo(7.63f, 12.237f, 7.63f, 11.763f, 7.923f, 11.47f)
                lineTo(14.173f, 5.22f)
                curveTo(14.466f, 4.927f, 14.941f, 4.927f, 15.233f, 5.22f)
                close()
            }
        }.build()

        return _Back!!
    }

@Suppress("ObjectPropertyName")
private var _Back: ImageVector? = null
