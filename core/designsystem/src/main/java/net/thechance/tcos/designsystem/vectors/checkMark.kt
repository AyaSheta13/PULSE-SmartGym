package net.thechance.tcos.designsystem.vectors

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val CheckMark: ImageVector
    get() {
        if (_CheckMark != null) {
            return _CheckMark!!
        }
        _CheckMark = ImageVector.Builder(
            name = "CheckMark",
            defaultWidth = 14.dp,
            defaultHeight = 14.dp,
            viewportWidth = 14f,
            viewportHeight = 14f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFFFCFCFC)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.917f, 7.583f)
                lineTo(5.25f, 9.917f)
                lineTo(11.083f, 4.083f)
            }
        }.build()

        return _CheckMark!!
    }

@Suppress("ObjectPropertyName")
private var _CheckMark: ImageVector? = null


@Preview
@Composable
private fun IconPreview(){
    Image(
        imageVector = CheckMark,
        contentDescription = "Icon Preview"
    )
}