package dev.nstv.sheepit.gestures.util

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring

private const val defaultDanceSpringStiffness = Spring.StiffnessMedium
private const val defaultDanceSpringDampingRatio = Spring.DampingRatioMediumBouncy

fun <T> defaultDanceAnimationSpec() = spring<T>(
    stiffness = defaultDanceSpringStiffness,
    dampingRatio = defaultDanceSpringDampingRatio,
)

fun sideToSideKeyframes(
    start: Float,
    diff: Float,
    duration: Int = 500,
) = keyframes {
    durationMillis = duration
    start atFraction 0.0f
    start + diff atFraction 0.25f
    start atFraction 0.5f
    start - diff atFraction 0.75f
    start atFraction 1.0f
}

fun <T> backAndForthKeyframes(
    start: T,
    temporaryValue: T,
    duration: Int = 500,
) = keyframes {
    durationMillis = duration
    start atFraction 0.0f
    temporaryValue atFraction 0.25f
    start atFraction 0.5f
    temporaryValue atFraction 0.75f
    start atFraction 1.0f
}