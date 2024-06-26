package dev.nstv.sheepit.gestures.sensormanager.modifiers

import android.hardware.Sensor.TYPE_LINEAR_ACCELERATION
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import dev.nstv.sheepit.gestures.sensormanager.AndroidSensorManager
import dev.nstv.sheepit.gestures.sensormanager.CUSTOM_ORIENTATION
import dev.nstv.sheepit.gestures.sensormanager.CUSTOM_ORIENTATION_CORRECTED
import dev.nstv.sheepit.gestures.sensormanager.DeviceOrientation
import dev.nstv.sheepit.gestures.sensormanager.Direction
import dev.nstv.sheepit.gestures.sensormanager.rememberSensorManager
import dev.nstv.sheepit.gestures.util.HalfPi
import dev.nstv.sheepit.gestures.util.ScreenSize
import dev.nstv.sheepit.gestures.util.backAndForthKeyframes
import dev.nstv.sheepit.gestures.util.mapRotation
import dev.nstv.sheepit.gestures.util.mapTranslationHeight
import dev.nstv.sheepit.gestures.util.mapTranslationWidth
import dev.nstv.sheepit.gestures.util.rememberScreenSize
import dev.nstv.sheepit.gestures.util.sideToSideKeyframes
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

    fun doOrientationDance(pitch: Float, roll: Float) {
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
                doOrientationDance(values[1], values[2])
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
 * Dance Parallax Modifier
 */
@Composable
fun Modifier.danceParallax(
    enabled: Boolean = true,
    danceEnabled: Boolean = true,
    adjusted: Boolean = true,
    onOrientationChanged: ((DeviceOrientation) -> Unit) = {},
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val sensorManager: AndroidSensorManager = rememberSensorManager()

    val translation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val sensors =
        remember(adjusted) { listOf(if (adjusted) CUSTOM_ORIENTATION_CORRECTED else CUSTOM_ORIENTATION) }

    fun doOrientationDance(roll: Float, pitch: Float) {
        // translation
        coroutineScope.launch {
            translation.animateTo(Offset(x = roll * 180f, y = pitch * 180f))
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
    shakeSensitivity: Float = 8f,
    shakeStopDelay: Long = 100,
    onShakeStarted: () -> Unit = {},
    onShakeStopped: () -> Unit = {},
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val sensorManager: AndroidSensorManager = rememberSensorManager()
    val screenSize: ScreenSize = rememberScreenSize()
    val moveDelta = screenSize.widthPx * 0.33f

    val scale = remember { Animatable(1f) }
    val translationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }

    var lastAcceleration: Float = SensorManager.GRAVITY_EARTH
    var acceleration: Float = 0f
    var isShaking = false
    var lastShakeTime: Long = 0

    fun doShakeStartedMove() {
        // No op
    }

    fun doShakeStoppedMove() {
        // Translation
        coroutineScope.launch {
            translationX.animateTo(
                targetValue = 0f,
                animationSpec = sideToSideKeyframes(0f, moveDelta)
            )
        }
        //Scale
        coroutineScope.launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = backAndForthKeyframes(1f, 0.5f)
            )
        }

        // Rotation
        coroutineScope.launch {
            rotationY.animateTo(
                targetValue = 0f,
                animationSpec = sideToSideKeyframes(0f, 90f)
            )
        }
    }

    val sensorModifier = if (enabled) {
        DanceMoveElement {
            this.translationX = translationX.value
            this.rotationY = rotationY.value
            this.scaleX = scale.value
            this.scaleY = scale.value
        }.onSensorEvent(
            sensors = listOf(TYPE_LINEAR_ACCELERATION),
            sensorManager = sensorManager,
        ) { _, values ->
            val x = values[0]
            val y = values[1]
            val z = values[2]

            // source: https://stackoverflow.com/questions/2317428/how-to-refresh-app-upon-shaking-the-device
            val currentAcceleration = sqrt(x * x + y * y + z * z)
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta
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
                    doShakeStoppedMove()
                }
            }
            lastAcceleration = currentAcceleration
        }
    } else {
        Modifier
    }

    return this then sensorModifier
}

/**
 * On X axis Shake Modifier
 */
@Composable
fun Modifier.onXShakeModifier(
    enabled: Boolean = true,
    danceEnabled: Boolean = true,
    shakeSensitivity: Float = 8f,
    shakeStopDelay: Long = 100,
    accelerationThreshold: Float = 10.0f,
    onShakeStarted: (Direction) -> Unit = {},
    onShakeStopped: (Direction) -> Unit = {},
): Modifier {
    val sensitivitySquared = shakeSensitivity * shakeSensitivity

    val coroutineScope = rememberCoroutineScope()
    val sensorManager: AndroidSensorManager = rememberSensorManager()
    val screenSize: ScreenSize = rememberScreenSize()
    val moveDelta = screenSize.widthPx * 0.33f

    val scale = remember { Animatable(1f) }
    val translationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }

    var lastAcceleration: Float = SensorManager.GRAVITY_EARTH
    var acceleration: Float = 0f
    var isShaking = false
    var lastShakeTime: Long = 0
    var direction = Direction.LEFT

    fun doShakeStartedMove() {
        // No op
    }

    fun doShakeStoppedMove(direction: Direction) {
        // Translation
        coroutineScope.launch {
            val move = if (direction == Direction.LEFT) -moveDelta else moveDelta
            translationX.animateTo(
                targetValue = 0f,
                animationSpec = backAndForthKeyframes(0f, move)
            )
        }
    }

    val sensorModifier = if (enabled) {
        DanceMoveElement {
            this.translationX = translationX.value
            this.rotationY = rotationY.value
            this.scaleX = scale.value
            this.scaleY = scale.value
        }.onSensorEvent(
            sensors = listOf(TYPE_LINEAR_ACCELERATION),
            sensorManager = sensorManager,
        ) { _, values ->
            val x = values[0]
            val y = values[1]
            val z = values[2]

            // source: https://stackoverflow.com/questions/2317428/how-to-refresh-app-upon-shaking-the-device
            val currentAcceleration = x * x + y * y + z * z
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta
            if (acceleration > sensitivitySquared && abs(x) > accelerationThreshold) {
                lastShakeTime = System.currentTimeMillis()
                isShaking = true
                direction = if (x > 0) Direction.RIGHT else Direction.LEFT
                onShakeStarted(direction)
                if (danceEnabled) {
                    doShakeStartedMove()
                }
            } else if (isShaking && System.currentTimeMillis() - lastShakeTime > shakeStopDelay) {
                isShaking = false
                onShakeStopped(direction)
                if (danceEnabled) {
                    doShakeStoppedMove(direction)
                }
            }
            lastAcceleration = currentAcceleration
        }
    } else {
        Modifier
    }

    return this then sensorModifier
}

/**
 * On axis Shake Modifier
 */
@Composable
fun Modifier.onAxisShakeModifier(
    enabled: Boolean = true,
    danceEnabled: Boolean = true,
    shakeSensitivity: Float = 8f,
    shakeStopDelay: Long = 100,
    accelerationThreshold: Float = 5.0f,
    onShakeStarted: (Direction) -> Unit = {},
    onShakeStopped: (Direction) -> Unit = {},
): Modifier {
    val sensitivitySquared = shakeSensitivity * shakeSensitivity

    val coroutineScope = rememberCoroutineScope()
    val sensorManager: AndroidSensorManager = rememberSensorManager()
    val screenSize: ScreenSize = rememberScreenSize()
    val moveDelta = screenSize.widthPx * 0.33f

    val scale = remember { Animatable(1f) }
    val translationX = remember { Animatable(0f) }
    val translationY = remember { Animatable(0f) }

    var lastAcceleration: Float = SensorManager.GRAVITY_EARTH
    var acceleration: Float = 0f
    var isShaking = false
    var lastShakeTime: Long = 0
    var direction = Direction.NONE

    fun doShakeStartedMove() {
        // No op
    }

    fun doShakeStoppedMove() {
        // Translation
        coroutineScope.launch {
            when (direction) {
                Direction.LEFT -> {
                    translationX.animateTo(
                        targetValue = 0f,
                        animationSpec = backAndForthKeyframes(0f, -moveDelta)
                    )
                }

                Direction.RIGHT -> {
                    translationX.animateTo(
                        targetValue = 0f,
                        animationSpec = backAndForthKeyframes(0f, moveDelta)
                    )
                }

                Direction.UP -> {
                    translationY.animateTo(
                        targetValue = 0f,
                        animationSpec = backAndForthKeyframes(0f, -moveDelta)
                    )
                }

                Direction.DOWN -> {
                    translationY.animateTo(
                        targetValue = 0f,
                        animationSpec = backAndForthKeyframes(0f, moveDelta)
                    )
                }

                Direction.FORWARD -> {
                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = backAndForthKeyframes(1f, 0.5f)
                        )
                    }
                    coroutineScope.launch {
                        translationY.animateTo(
                            targetValue = 0f,
                            animationSpec = backAndForthKeyframes(0f, -moveDelta)
                        )
                    }
                }

                Direction.BACKWARD -> {
                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = backAndForthKeyframes(1f, 0.5f)
                        )
                    }
                    coroutineScope.launch {
                        translationY.animateTo(
                            targetValue = 0f,
                            animationSpec = backAndForthKeyframes(0f, moveDelta)
                        )
                    }

                }

                Direction.NONE -> {} // No op
            }
        }
    }

    fun getDirection(x: Float, y: Float, z: Float): Direction {
        val absoluteX = abs(x)
        val absoluteY = abs(y)
        val absoluteZ = abs(z)

        if (absoluteX < accelerationThreshold && absoluteY < accelerationThreshold && absoluteZ < accelerationThreshold) {
            return Direction.NONE
        }

        return when {
            absoluteX > absoluteY && absoluteX > absoluteZ -> {
                if (x < 0) Direction.RIGHT else Direction.LEFT
            }

            absoluteY > absoluteX && absoluteY > absoluteZ -> {
                if (y > 0) Direction.UP else Direction.DOWN
            }

            else -> {
                if (z > 0) Direction.FORWARD else Direction.BACKWARD
            }
        }
    }

    val sensorModifier = if (enabled) {
        DanceMoveElement {
            this.translationX = translationX.value
            this.translationY = translationY.value
            this.scaleX = scale.value
            this.scaleY = scale.value
        }.onSensorEvent(
            sensors = listOf(TYPE_LINEAR_ACCELERATION),
            sensorManager = sensorManager,
        ) { _, values ->
            val x = values[0]
            val y = values[1]
            val z = values[2]

            // source: https://stackoverflow.com/questions/2317428/how-to-refresh-app-upon-shaking-the-device
            val currentAcceleration = x * x + y * y + z * z
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta
            if (acceleration > sensitivitySquared) {
                lastShakeTime = System.currentTimeMillis()
                isShaking = true
                direction = getDirection(x, y, z)
                onShakeStarted(direction)
                if (danceEnabled) {
                    doShakeStartedMove()
                }
            } else if (isShaking && System.currentTimeMillis() - lastShakeTime > shakeStopDelay) {
                isShaking = false
                onShakeStopped(direction)
                if (danceEnabled) {
                    doShakeStoppedMove()
                }
            }
            lastAcceleration = currentAcceleration
        }
    } else {
        Modifier
    }

    return this then sensorModifier
}

