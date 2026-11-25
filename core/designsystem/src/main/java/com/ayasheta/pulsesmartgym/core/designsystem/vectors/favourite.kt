package com.ayasheta.pulsesmartgym.core.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Favourite: ImageVector
    get() {
        if (_Favourite != null) {
            return _Favourite!!
        }
        _Favourite = ImageVector.Builder(
            name = "Favourite",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF343C54)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(11.823f, 4.771f)
                lineTo(12f, 4.949f)
                lineTo(12.177f, 4.771f)
                curveTo(14.424f, 2.524f, 18.068f, 2.524f, 20.315f, 4.771f)
                curveTo(22.562f, 7.018f, 22.562f, 10.662f, 20.315f, 12.909f)
                lineTo(13.591f, 19.632f)
                curveTo(12.712f, 20.511f, 11.288f, 20.511f, 10.409f, 19.632f)
                lineTo(3.685f, 12.909f)
                curveTo(1.438f, 10.661f, 1.438f, 7.018f, 3.685f, 4.771f)
                curveTo(5.932f, 2.524f, 9.576f, 2.524f, 11.823f, 4.771f)
                close()
                moveTo(10.762f, 5.832f)
                curveTo(9.101f, 4.171f, 6.407f, 4.171f, 4.746f, 5.832f)
                curveTo(3.085f, 7.493f, 3.085f, 10.187f, 4.746f, 11.848f)
                lineTo(11.47f, 18.572f)
                curveTo(11.762f, 18.865f, 12.237f, 18.865f, 12.53f, 18.572f)
                lineTo(19.254f, 11.848f)
                curveTo(20.915f, 10.187f, 20.915f, 7.493f, 19.254f, 5.832f)
                curveTo(17.593f, 4.171f, 14.899f, 4.171f, 13.238f, 5.832f)
                lineTo(12.53f, 6.54f)
                curveTo(12.39f, 6.68f, 12.199f, 6.759f, 12f, 6.759f)
                curveTo(11.801f, 6.759f, 11.61f, 6.68f, 11.47f, 6.54f)
                lineTo(10.762f, 5.832f)
                close()
            }
        }.build()

        return _Favourite!!
    }

@Suppress("ObjectPropertyName")
private var _Favourite: ImageVector? = null


internal val FavouriteFilled: ImageVector
    get() {
        if (_FavouriteFilled != null) {
            return _FavouriteFilled!!
        }
        _FavouriteFilled = ImageVector.Builder(
            name = "FavouriteFilled",
            defaultWidth = 80.dp,
            defaultHeight = 80.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF343C54))) {
                moveTo(11.823f, 4.772f)
                curveTo(9.576f, 2.525f, 5.932f, 2.525f, 3.685f, 4.772f)
                curveTo(1.438f, 7.019f, 1.438f, 10.663f, 3.685f, 12.91f)
                lineTo(10.409f, 19.633f)
                curveTo(11.288f, 20.512f, 12.712f, 20.512f, 13.591f, 19.633f)
                lineTo(20.315f, 12.91f)
                curveTo(22.562f, 10.663f, 22.562f, 7.019f, 20.315f, 4.772f)
                curveTo(18.068f, 2.525f, 14.424f, 2.525f, 12.177f, 4.772f)
                lineTo(12f, 4.95f)
                lineTo(11.823f, 4.772f)
                close()
            }
        }.build()

        return _FavouriteFilled!!
    }

@Suppress("ObjectPropertyName")
private var _FavouriteFilled: ImageVector? = null
