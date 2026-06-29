package com.example.dymnbaby

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object EventIcons {
    val Sleep: ImageVector by lazy {
        ImageVector.Builder("Sleep", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.7f, 15.8f)
                curveTo(14.3f, 16.6f, 10.2f, 13.2f, 10.2f, 8.6f)
                curveTo(10.2f, 6.9f, 10.7f, 5.3f, 11.6f, 4f)
                curveTo(7.2f, 4.4f, 3.8f, 8.1f, 4f, 12.6f)
                curveTo(4.2f, 17.2f, 8f, 20.8f, 12.6f, 20.8f)
                curveTo(15.5f, 20.8f, 18.1f, 19.2f, 19.5f, 16.9f)
                curveTo(19.8f, 16.3f, 19.3f, 15.7f, 18.7f, 15.8f)
                close()
            }
        }.build()
    }

    val Feeding: ImageVector by lazy {
        ImageVector.Builder("Feeding", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 2.5f)
                lineTo(15f, 2.5f)
                lineTo(15f, 5f)
                lineTo(16.5f, 6.5f)
                lineTo(16.5f, 19f)
                curveTo(16.5f, 20.4f, 15.4f, 21.5f, 14f, 21.5f)
                lineTo(10f, 21.5f)
                curveTo(8.6f, 21.5f, 7.5f, 20.4f, 7.5f, 19f)
                lineTo(7.5f, 6.5f)
                lineTo(9f, 5f)
                close()
                moveTo(9.5f, 9f)
                lineTo(14.5f, 9f)
                lineTo(14.5f, 11f)
                lineTo(9.5f, 11f)
                close()
                moveTo(9.5f, 14f)
                lineTo(14.5f, 14f)
                lineTo(14.5f, 16f)
                lineTo(9.5f, 16f)
                close()
            }
        }.build()
    }

    val Active: ImageVector by lazy {
        ImageVector.Builder("Active", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 7f)
                curveTo(9.2f, 7f, 7f, 9.2f, 7f, 12f)
                curveTo(7f, 14.8f, 9.2f, 17f, 12f, 17f)
                curveTo(14.8f, 17f, 17f, 14.8f, 17f, 12f)
                curveTo(17f, 9.2f, 14.8f, 7f, 12f, 7f)
                close()
                moveTo(11.2f, 2f)
                lineTo(12.8f, 2f)
                lineTo(12.8f, 5f)
                lineTo(11.2f, 5f)
                close()
                moveTo(11.2f, 19f)
                lineTo(12.8f, 19f)
                lineTo(12.8f, 22f)
                lineTo(11.2f, 22f)
                close()
                moveTo(2f, 11.2f)
                lineTo(5f, 11.2f)
                lineTo(5f, 12.8f)
                lineTo(2f, 12.8f)
                close()
                moveTo(19f, 11.2f)
                lineTo(22f, 11.2f)
                lineTo(22f, 12.8f)
                lineTo(19f, 12.8f)
                close()
                moveTo(4.5f, 5.6f)
                lineTo(5.6f, 4.5f)
                lineTo(7.8f, 6.7f)
                lineTo(6.7f, 7.8f)
                close()
                moveTo(16.2f, 6.7f)
                lineTo(18.4f, 4.5f)
                lineTo(19.5f, 5.6f)
                lineTo(17.3f, 7.8f)
                close()
                moveTo(4.5f, 18.4f)
                lineTo(6.7f, 16.2f)
                lineTo(7.8f, 17.3f)
                lineTo(5.6f, 19.5f)
                close()
                moveTo(16.2f, 17.3f)
                lineTo(17.3f, 16.2f)
                lineTo(19.5f, 18.4f)
                lineTo(18.4f, 19.5f)
                close()
            }
        }.build()
    }

    val Toilet: ImageVector by lazy {
        ImageVector.Builder("Toilet", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 5f)
                lineTo(19f, 5f)
                curveTo(19f, 7.2f, 17.5f, 9f, 15.5f, 9.8f)
                lineTo(15.5f, 14.5f)
                curveTo(15.5f, 16.4f, 14f, 18f, 12f, 18f)
                curveTo(10f, 18f, 8.5f, 16.4f, 8.5f, 14.5f)
                lineTo(8.5f, 9.8f)
                curveTo(6.5f, 9f, 5f, 7.2f, 5f, 5f)
                close()
                moveTo(7f, 19f)
                lineTo(17f, 19f)
                curveTo(17.8f, 19f, 18.5f, 19.7f, 18.5f, 20.5f)
                lineTo(18.5f, 21f)
                lineTo(5.5f, 21f)
                lineTo(5.5f, 20.5f)
                curveTo(5.5f, 19.7f, 6.2f, 19f, 7f, 19f)
                close()
            }
        }.build()
    }

    val Measurement: ImageVector by lazy {
        ImageVector.Builder("Measurement", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 6f)
                curveTo(4f, 4.9f, 4.9f, 4f, 6f, 4f)
                lineTo(18f, 4f)
                curveTo(19.1f, 4f, 20f, 4.9f, 20f, 6f)
                lineTo(20f, 18f)
                curveTo(20f, 19.1f, 19.1f, 20f, 18f, 20f)
                lineTo(6f, 20f)
                curveTo(4.9f, 20f, 4f, 19.1f, 4f, 18f)
                close()
                moveTo(7f, 8f)
                lineTo(17f, 8f)
                lineTo(17f, 10f)
                lineTo(15f, 10f)
                lineTo(15f, 13f)
                lineTo(13f, 13f)
                lineTo(13f, 10f)
                lineTo(11f, 10f)
                lineTo(11f, 12f)
                lineTo(9f, 12f)
                lineTo(9f, 10f)
                lineTo(7f, 10f)
                close()
            }
        }.build()
    }

    val Walk: ImageVector by lazy {
        ImageVector.Builder("Walk", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 3f)
                curveTo(10.2f, 3f, 11.5f, 5.1f, 11.5f, 7.2f)
                curveTo(11.5f, 9.3f, 10.2f, 11f, 8.4f, 11f)
                curveTo(6.5f, 11f, 5f, 9.3f, 5f, 7.2f)
                curveTo(5f, 5.1f, 5.8f, 3f, 8f, 3f)
                close()
                moveTo(16.3f, 13f)
                curveTo(18.2f, 13f, 19f, 14.8f, 19f, 16.8f)
                curveTo(19f, 19f, 17.6f, 21f, 15.5f, 21f)
                curveTo(13.5f, 21f, 12f, 19.2f, 12f, 17.2f)
                curveTo(12f, 15.1f, 14.2f, 13f, 16.3f, 13f)
                close()
            }
        }.build()
    }

    val Milestone: ImageVector by lazy {
        ImageVector.Builder("Milestone", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 3f)
                lineTo(7f, 3f)
                lineTo(7f, 21f)
                lineTo(5f, 21f)
                close()
                moveTo(8f, 4f)
                lineTo(19f, 4f)
                lineTo(16f, 8f)
                lineTo(19f, 12f)
                lineTo(8f, 12f)
                close()
            }
        }.build()
    }
}
