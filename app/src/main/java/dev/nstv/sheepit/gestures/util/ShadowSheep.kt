package dev.nstv.sheepit.gestures.util

import androidx.compose.ui.graphics.Color
import dev.nstv.composablesheep.library.model.Sheep

val shadowColor = Color.Gray.copy(alpha = 0.5f)

val ShadowSheep = Sheep(
    fluffColor = shadowColor,
    headColor = shadowColor,
    legColor = shadowColor,
    glassesColor = shadowColor
)