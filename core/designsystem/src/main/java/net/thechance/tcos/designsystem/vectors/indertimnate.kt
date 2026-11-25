package net.thechance.tcos.designsystem.vectors

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val Indeterminate: ImageVector
    get() {
        if (_Indeterminate != null) {
            return _Indeterminate!!
        }
        _Indeterminate = ImageVector.Builder(
            name = "Indeterminate",
            defaultWidth = 14.dp,
            defaultHeight = 14.dp,
            viewportWidth = 14f,
            viewportHeight = 14f
        ).apply {
            path(fill = SolidColor(Color(0xFFFCFCFC))) {
                moveTo(2f, 7f)
                curveTo(2f, 6.448f, 2.448f, 6f, 3f, 6f)
                horizontalLineTo(11f)
                curveTo(11.552f, 6f, 12f, 6.448f, 12f, 7f)
                curveTo(12f, 7.552f, 11.552f, 8f, 11f, 8f)
                horizontalLineTo(3f)
                curveTo(2.448f, 8f, 2f, 7.552f, 2f, 7f)
                close()
            }
        }.build()

        return _Indeterminate!!
    }

@Suppress("ObjectPropertyName")
private var _Indeterminate: ImageVector? = null

@Preview
@Composable
private fun IconPreview(){
    Image(
        imageVector = Indeterminate,
        contentDescription = "Icon Preview"
    )
}