package dev.nstv.sheepit.gestures.sensormanager.modifiers

import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.Sensor.TYPE_LINEAR_ACCELERATION
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import dev.nstv.sheepit.gestures.sensormanager.AndroidSensorManager
import dev.nstv.sheepit.gestures.sensormanager.CUSTOM_ORIENTATION
import dev.nstv.sheepit.gestures.sensormanager.CUSTOM_ORIENTATION_CORRECTED
import dev.nstv.sheepit.gestures.sensormanager.DeviceOrientation
import dev.nstv.sheepit.gestures.sensormanager.rememberSensorManager
import dev.nstv.sheepit.gestures.util.HalfPi
import dev.nstv.sheepit.gestures.util.ScreenSize
import dev.nstv.sheepit.gestures.util.mapRotation
import dev.nstv.sheepit.gestures.util.mapTranslationHeight
import dev.nstv.sheepit.gestures.util.mapTranslationWidth
import dev.nstv.sheepit.gestures.util.rememberScreenSize
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt


/**
 * Dance Orientation Change Modifier
 */
@Composable
fun Modifier.danceOrientationChange(
    enabled: Boolean = true,
    danceEnabled: Boolean = true,
    adjusted: Boolean = false,
    onOrientationChanged: ((DeviceOrientation) -> Unit) = {},
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val sensorManager: AndroidSensorManager = rememberSensorManager()
    val screenSize: ScreenSize = rememberScreenSize()

    val rotation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val translation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val sensors =
        remember(adjusted) { listOf(if (adjusted) CUSTOM_ORIENTATION_CORRECTED else CUSTOM_ORIENTATION) }

    fun doOrientationDance(roll: Float, pitch: Float) {
        // Rotation
        coroutineScope.launch {
            val degreesX = mapRotation(pitch, HalfPi)
            val degreesY = mapRotation(roll)
            rotation.animateTo(Offset(x = degreesX, y = degreesY))
        }

        // Styled movement
        coroutineScope.launch {
            val offsetX = mapTranslationWidth(roll, screenSize)
            val offsetY = mapTranslationHeight(pitch, screenSize, HalfPi)
            translation.animateTo(Offset(x = offsetX, y = -offsetY))
        }
    }

    val sensorModifier = if (enabled) {
        onSensorEvent(
            sensors = sensors,
            sensorManager = sensorManager,
        ) { _, values ->
            if (danceEnabled) {
                doOrientationDance(values[2], values[1])
            }
            onOrientationChanged(DeviceOrientation(values))
        }.then(DanceMoveElement {
            translationX = translation.value.x
            translationY = translation.value.y
            rotationX = rotation.value.x
            rotationY = rotation.value.y
        })
    } else Modifier

    return this then sensorModifier
}

/**
 * Dance Shake Modifier
 */
@Composable
fun Modifier.danceShakeModifier(
    enabled: Boolean = true,
    danceEnabled: Boolean = true,
    shakeSensitivity: Float = 12f,
    shakeStopDelay: Long = 200,
    onShakeStarted: () -> Unit = {},
    onShakeStopped: () -> Unit = {},
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val sensorManager: AndroidSensorManager = rememberSensorManager()
    val screenSize: ScreenSize = rememberScreenSize()
    val moveDelta = screenSize.widthPx * 0.25f

    val scale = remember { Animatable(1f) }
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }
    val translationX = remember { Animatable(0f) }
    val translationY = remember { Animatable(0f) }

    var lastAccelerationValues = floatArrayOf(0f, 0f, 0f)
    var lastAcceleration: Float = SensorManager.GRAVITY_EARTH
    var isShaking = false
    var lastShakeTime: Long = 0

    fun doShakeStartedMove() {
        // No op
    }

    fun doShakeStoppedMove(
        xDiff: Float,
        yDiff: Float,
        zDiff: Float,
    ) {
        if (zDiff > xDiff && zDiff > yDiff) {
            coroutineScope.launch {
                scale.animateTo(
                    1f,
                    keyframes {
                        durationMillis = 500
                        1f atFraction 0.0f
                        0.5f atFraction 0.25f
                        1f atFraction 0.5f
                        1.5f atFraction 0.75f
                        1f atFraction 1.0f
                    }
                )
            }
            coroutineScope.launch {
                translationY.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 500
                        0f atFraction 0.0f
                        moveDelta / 2 atFraction 0.25f
                        0f atFraction 0.5f
                        -moveDelta / 2 atFraction 0.75f
                        0f atFraction 1.0f
                    }
                )
            }
        } else {
            val translation = if (xDiff > yDiff) translationX else translationY
            // Translation
            coroutineScope.launch {
                translation.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 500
                        0f atFraction 0.0f
                        moveDelta atFraction 0.25f
                        0f atFraction 0.5f
                        -moveDelta atFraction 0.75f
                        0f atFraction 1.0f
                    }
                )
            }
            //Scale
            coroutineScope.launch {
                scale.animateTo(
                    1f,
                    keyframes {
                        durationMillis = 500
                        1f atFraction 0.0f
                        0.5f atFraction 0.25f
                        1f atFraction 0.5f
                        0.5f atFraction 0.75f
                        1f atFraction 1.0f
                    }
                )
            }
//            // Rotation
//            coroutineScope.launch {
//                rotation.animateTo(
//                    targetValue = 0f,
//                    animationSpec = keyframes {
//                        durationMillis = 500
//                        0f atFraction 0.0f
//                        90f atFraction 0.25f
//                        0f atFraction 0.5f
//                        -90f atFraction 0.75f
//                        0f atFraction 1.0f
//                    }
//                )
//            }
        }
    }

    val sensorModifier = if (enabled) {
        DanceMoveElement {
            this.translationX = translationX.value
            this.translationY = translationY.value
            this.rotationX = rotationX.value
            this.rotationY = rotationY.value
            this.scaleX = scale.value
            this.scaleY = scale.value
        }.onSensorEvent(
            sensors = listOf(TYPE_ACCELEROMETER),
            sensorManager = sensorManager,
        ) { _, values ->
            val x = values[0]
            val y = values[1]
            val z = values[2]

            // 3D vector length (Euclidean norm aka n-Pythagoras)
            val currentAcceleration = sqrt(x * x + y * y + z * z)
            val delta: Float = currentAcceleration - lastAcceleration
            val acceleration = currentAcceleration * 0.9f + delta
            if (acceleration > shakeSensitivity) {
                lastShakeTime = System.currentTimeMillis()
                isShaking = true
                onShakeStarted()
                if (danceEnabled) {
                    doShakeStartedMove()
                }
            } else if (isShaking && System.currentTimeMillis() - lastShakeTime > shakeStopDelay) {
                isShaking = false
                onShakeStopped()
                if (danceEnabled) {
                    doShakeStoppedMove(
                        xDiff = x - lastAccelerationValues[0],
                        yDiff = y - lastAccelerationValues[1],
                        zDiff = z - lastAccelerationValues[2],
                    )
                }
            }
            lastAcceleration = currentAcceleration
            lastAccelerationValues = floatArrayOf(x, y, z)
        }
    } else {
        Modifier
    }

    return this then sensorModifier
}


