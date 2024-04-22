package dev.nstv.sheepit.gestures.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import dev.nstv.composablesheep.library.util.toRadians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Constants
const val SensorMagnitude = 500f
const val AccelerationThreshold = 1f
const val DegreesThreshold = 3f
const val RotationDegreesThreshold = 10f
const val DefaultDelay = 100L

// PI
const val Pi = PI.toFloat()
const val HalfPi = Pi / 2f
const val TwoPi = Pi * 2f
val PiPlusThreshold = Pi + RotationDegreesThreshold.toRadians()
val PiMinusThreshold = Pi - RotationDegreesThreshold.toRadians()

// Extensions
fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt())
fun Double.toDegrees() = 180.0 * this / PI
fun Float.toDegrees() = (180.0 * this / PI).toFloat()

// Functions
fun mapValues(
    value: Float,
    fromStart: Float,
    fromEnd: Float,
    toStart: Float,
    toEnd: Float,
) = (value - fromStart) / (fromEnd - fromStart) * (toEnd - toStart) + toStart

fun mapRotation(
    angle: Float,
    maxValue: Float = Pi,
) = mapValues(
    value = angle,
    fromStart = -maxValue,
    fromEnd = maxValue,
    toStart = -90f,
    toEnd = 90f,
)

fun mapTranslationWidth(
    angle: Float,
    screenSize: ScreenSize,
    maxValue: Float = Pi,
) = mapValues(
    value = angle,
    fromStart = -maxValue,
    fromEnd = maxValue,
    toStart = -screenSize.widthPx / 2f,
    toEnd = screenSize.widthPx / 2f,
)

fun mapTranslationHeight(
    angle: Float,
    screenSize: ScreenSize,
    maxValue: Float = Pi,
) = mapValues(
    value = angle,
    fromStart = -maxValue,
    fromEnd = maxValue,
    toStart = -screenSize.heightPx / 2f,
    toEnd = screenSize.heightPx / 2f,
)

fun Offset.height() = y
fun Offset.width() = x

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}

// based on: https://medium.com/@rahulbehera/sensor-based-parallax-animations-for-android-and-ios-5363f38e37ec
fun getOrientationGimbalLockCorrected(
    lastRotationReading: FloatArray,
    currentRotationReading: FloatArray,
) = FloatArray(3) { index ->
    when (index) {
        0 -> (lastRotationReading[0] * currentRotationReading[1] + lastRotationReading[3] * currentRotationReading[4] + lastRotationReading[6] * currentRotationReading[7]) * Pi // azimuth[-z] (0 to 2π)
        1 -> -(lastRotationReading[2] * currentRotationReading[1] + lastRotationReading[5] * currentRotationReading[4] + lastRotationReading[8] * currentRotationReading[7]) * HalfPi// pitch[x] (-π/2 to π/2)
        2 -> -(lastRotationReading[2] * currentRotationReading[0] + lastRotationReading[5] * currentRotationReading[3] + lastRotationReading[8] * currentRotationReading[6]) * Pi// roll[y] (-π to π)
        else -> 0f
    }
}

