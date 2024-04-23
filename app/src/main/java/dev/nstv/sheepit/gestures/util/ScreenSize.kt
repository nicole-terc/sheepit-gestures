package dev.nstv.sheepit.gestures.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

data class ScreenSize(
    val width: Dp,
    val height: Dp,
    val widthPx: Int,
    val heightPx: Int
) {
    val middlePoint: DpOffset = DpOffset(width / 2, height / 2)
    val halfSize: Offset = Offset(widthPx / 2f, heightPx / 2f)
}

@Composable
fun rememberScreenSize(): ScreenSize {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    return remember(density, configuration) {
        val width = configuration.screenWidthDp.dp
        val height = configuration.screenHeightDp.dp
        ScreenSize(
            width = width,
            height = height,
            widthPx = with(density) { width.roundToPx() },
            heightPx = with(density) { height.roundToPx() },
        )
    }
}