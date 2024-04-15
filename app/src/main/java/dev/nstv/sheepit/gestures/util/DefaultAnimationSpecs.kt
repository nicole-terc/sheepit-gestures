package dev.nstv.sheepit.gestures.util

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

private const val defaultDanceSpringStiffness = Spring.StiffnessMedium
private const val defaultDanceSpringDampingRatio = Spring.DampingRatioMediumBouncy

fun <T> defaultDanceAnimationSpec() = spring<T>(
    stiffness = defaultDanceSpringStiffness,
    dampingRatio = defaultDanceSpringDampingRatio,
)
