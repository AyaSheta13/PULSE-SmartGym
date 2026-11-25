package com.ayasheta.pulsesmartgym.core.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Close: ImageVector
    get() {
        if (_Close != null) {
            return _Close!!
        }
        _Close = ImageVector.Builder(
            name = "Close",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(fill = SolidColor(Color(0xFF717779))) {
                moveTo(7.942f, 7.058f)
                curveTo(7.698f, 6.814f, 7.302f, 6.814f, 7.058f, 7.058f)
                curveTo(6.814f, 7.302f, 6.814f, 7.698f, 7.058f, 7.942f)
                lineTo(9.116f, 10f)
                lineTo(7.059f, 12.058f)
                curveTo(6.814f, 12.302f, 6.815f, 12.698f, 7.059f, 12.942f)
                curveTo(7.303f, 13.186f, 7.698f, 13.186f, 7.942f, 12.942f)
                lineTo(10f, 10.884f)
                lineTo(12.057f, 12.942f)
                curveTo(12.302f, 13.186f, 12.697f, 13.186f, 12.941f, 12.942f)
                curveTo(13.186f, 12.698f, 13.186f, 12.302f, 12.941f, 12.058f)
                lineTo(10.884f, 10f)
                lineTo(12.942f, 7.942f)
                curveTo(13.186f, 7.698f, 13.186f, 7.302f, 12.942f, 7.058f)
                curveTo(12.698f, 6.814f, 12.302f, 6.814f, 12.058f, 7.058f)
                lineTo(10f, 9.116f)
                lineTo(7.942f, 7.058f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF717779)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(10f, 18.958f)
                curveTo(5.052f, 18.958f, 1.042f, 14.948f, 1.042f, 10f)
                curveTo(1.042f, 5.052f, 5.052f, 1.042f, 10f, 1.042f)
                curveTo(14.948f, 1.042f, 18.958f, 5.052f, 18.958f, 10f)
                curveTo(18.958f, 14.948f, 14.948f, 18.958f, 10f, 18.958f)
                close()
                moveTo(2.292f, 10f)
                curveTo(2.292f, 14.257f, 5.743f, 17.708f, 10f, 17.708f)
                curveTo(14.257f, 17.708f, 17.708f, 14.257f, 17.708f, 10f)
                curveTo(17.708f, 5.743f, 14.257f, 2.292f, 10f, 2.292f)
                curveTo(5.743f, 2.292f, 2.292f, 5.743f, 2.292f, 10f)
                close()
            }
        }.build()

        return _Close!!
    }

@Suppress("ObjectPropertyName")
private var _Close: ImageVector? = null
