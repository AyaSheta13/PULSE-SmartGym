package com.ayasheta.pulsesmartgym.core.designsystem.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Search: ImageVector
    get() {
        if (_Search != null) return _Search!!
        _Search = ImageVector.Builder(
            name = "Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(11f, 19f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = false, 11f, 3f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = false, 11f, 19f)
                close()
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
        }.build()
        return _Search!!
    }
private var _Search: ImageVector? = null

val Bluetooth: ImageVector
    get() {
        if (_Bluetooth != null) return _Bluetooth!!
        _Bluetooth = ImageVector.Builder(
            name = "Bluetooth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 6.5f)
                lineTo(17.5f, 17.5f)
                lineTo(12f, 22f)
                verticalLineTo(2f)
                lineTo(17.5f, 6.5f)
                lineTo(6.5f, 17.5f)
            }
        }.build()
        return _Bluetooth!!
    }
private var _Bluetooth: ImageVector? = null

val Workouts: ImageVector
    get() {
        if (_Workouts != null) return _Workouts!!
        _Workouts = ImageVector.Builder(
            name = "Workouts",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 6.5f)
                verticalLineTo(17.5f)
                moveTo(17.5f, 6.5f)
                verticalLineTo(17.5f)
                moveTo(3.5f, 8.5f)
                verticalLineTo(15.5f)
                moveTo(20.5f, 8.5f)
                verticalLineTo(15.5f)
                moveTo(6.5f, 12f)
                horizontalLineTo(17.5f)
            }
        }.build()
        return _Workouts!!
    }
private var _Workouts: ImageVector? = null

val ProgressChart: ImageVector
    get() {
        if (_ProgressChart != null) return _ProgressChart!!
        _ProgressChart = ImageVector.Builder(
            name = "ProgressChart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 20f)
                horizontalLineTo(21f)
                moveTo(6f, 16f)
                verticalLineTo(11f)
                moveTo(12f, 16f)
                verticalLineTo(7f)
                moveTo(18f, 16f)
                verticalLineTo(4f)
            }
        }.build()
        return _ProgressChart!!
    }
private var _ProgressChart: ImageVector? = null

val HistoryClock: ImageVector
    get() {
        if (_HistoryClock != null) return _HistoryClock!!
        _HistoryClock = ImageVector.Builder(
            name = "HistoryClock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 21f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = false, 12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = false, 12f, 21f)
                close()
                moveTo(12f, 7f)
                verticalLineTo(12f)
                lineTo(15.5f, 14f)
            }
        }.build()
        return _HistoryClock!!
    }
private var _HistoryClock: ImageVector? = null

val VisibilityOn: ImageVector
    get() {
        if (_VisibilityOn != null) return _VisibilityOn!!
        _VisibilityOn = ImageVector.Builder(
            name = "VisibilityOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2f, 12f)
                curveTo(4.5f, 7f, 8f, 5f, 12f, 5f)
                curveTo(16f, 5f, 19.5f, 7f, 22f, 12f)
                curveTo(19.5f, 17f, 16f, 19f, 12f, 19f)
                curveTo(8f, 19f, 4.5f, 17f, 2f, 12f)
                close()
                moveTo(12f, 15f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 12f, 9f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 12f, 15f)
                close()
            }
        }.build()
        return _VisibilityOn!!
    }
private var _VisibilityOn: ImageVector? = null

val VisibilityOff: ImageVector
    get() {
        if (_VisibilityOff != null) return _VisibilityOff!!
        _VisibilityOff = ImageVector.Builder(
            name = "VisibilityOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 21f)
                moveTo(10.58f, 10.58f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 13.42f, 13.42f)
                moveTo(9.88f, 5.09f)
                curveTo(10.57f, 5.03f, 11.28f, 5f, 12f, 5f)
                curveTo(16f, 5f, 19.5f, 7f, 22f, 12f)
                curveTo(21.17f, 13.66f, 20.13f, 15.05f, 18.9f, 16.17f)
                moveTo(6.61f, 6.61f)
                curveTo(4.62f, 7.96f, 3.04f, 9.79f, 2f, 12f)
                curveTo(4.5f, 17f, 8f, 19f, 12f, 19f)
                curveTo(13.94f, 19f, 15.72f, 18.53f, 17.28f, 17.68f)
            }
        }.build()
        return _VisibilityOff!!
    }
private var _VisibilityOff: ImageVector? = null

val Play: ImageVector
    get() {
        if (_Play != null) return _Play!!
        _Play = ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 4f)
                lineTo(20f, 12f)
                lineTo(6f, 20f)
                close()
            }
        }.build()
        return _Play!!
    }
private var _Play: ImageVector? = null

val Plus: ImageVector
    get() {
        if (_Plus != null) return _Plus!!
        _Plus = ImageVector.Builder(
            name = "Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }.build()
        return _Plus!!
    }
private var _Plus: ImageVector? = null

val Minus: ImageVector
    get() {
        if (_Minus != null) return _Minus!!
        _Minus = ImageVector.Builder(
            name = "Minus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }.build()
        return _Minus!!
    }
private var _Minus: ImageVector? = null

val Trash: ImageVector
    get() {
        if (_Trash != null) return _Trash!!
        _Trash = ImageVector.Builder(
            name = "Trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 6f)
                horizontalLineTo(21f)
                moveTo(8f, 6f)
                verticalLineTo(4f)
                horizontalLineTo(16f)
                verticalLineTo(6f)
                moveTo(19f, 6f)
                verticalLineTo(20f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17f, 22f)
                horizontalLineTo(7f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 20f)
                verticalLineTo(6f)
            }
        }.build()
        return _Trash!!
    }
private var _Trash: ImageVector? = null

val ShareIcon: ImageVector
    get() {
        if (_ShareIcon != null) return _ShareIcon!!
        _ShareIcon = ImageVector.Builder(
            name = "ShareIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 12f)
                verticalLineTo(20f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 22f)
                horizontalLineTo(18f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20f, 20f)
                verticalLineTo(12f)
                moveTo(16f, 6f)
                lineTo(12f, 2f)
                lineTo(8f, 6f)
                moveTo(12f, 2f)
                verticalLineTo(15f)
            }
        }.build()
        return _ShareIcon!!
    }
private var _ShareIcon: ImageVector? = null
