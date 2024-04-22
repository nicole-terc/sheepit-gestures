package dev.nstv.sheepit.gestures.ui.screen

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.nstv.composablesheep.library.ComposableSheep
import dev.nstv.composablesheep.library.model.Sheep
import dev.nstv.composablesheep.library.util.SheepColor
import dev.nstv.sheepit.gestures.util.DefaultDelay
import dev.nstv.sheepit.gestures.util.HalfPi
import dev.nstv.sheepit.gestures.util.ScreenSize
import dev.nstv.sheepit.gestures.util.backAndForthKeyframes
import dev.nstv.sheepit.gestures.util.mapRotation
import dev.nstv.sheepit.gestures.util.mapTranslationHeight
import dev.nstv.sheepit.gestures.util.mapTranslationWidth
import dev.nstv.sheepit.gestures.util.rememberScreenSize
import dev.nstv.sheepit.gestures.util.sideToSideKeyframes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SheepScreen(
    modifier: Modifier = Modifier,
    sheep: Sheep = Sheep(fluffColor = SheepColor.Green),
    screenSize: ScreenSize = rememberScreenSize(),
) {

    val coroutineScope = rememberCoroutineScope()

    // Properties to animate
    // Step 1.1.1 scale
    val scale = remember { Animatable(1f) }

    // Step 2.1 rotationZ
    val rotationZ = remember { Animatable(0f) }

    // Step 6.1 rotationX and rotationY
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }

    // Step 4.1 translation offset
    val translation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    translation.updateBounds(
        lowerBound = Offset(-screenSize.halfSize.x, -screenSize.halfSize.y),
        upperBound = Offset(screenSize.halfSize.x, screenSize.halfSize.y),
    )

    // Dragging State & Decay
    // Step 4.2 Dragging state - Snap!
    val draggableState = rememberDraggable2DState { delta ->
        coroutineScope.launch {
            translation.snapTo(translation.value.plus(delta))
        }
    }

    // Step 5.2.3 Decay
    val decay = rememberSplineBasedDecay<Offset>()


    // Sensors
    // Step 6.1
    val context = LocalContext.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    // Step 8.1 Bonus
    val fluffColor = remember { Animatable(SheepColor.Green) }
    val glassesColor = remember { Animatable(SheepColor.Black) }

    LifecycleResumeEffect(sensorManager) {

        // Step 6.2 Rotation
        val rotationListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                Log.d(
                    "Rotation values: ", "" +
                            "x: ${event.values[0]} " +
                            "y: ${event.values[1]} " +
                            "z: ${event.values[2]} "
                )
                coroutineScope.launch {
                    rotationX.animateTo(event.values[0] * 180)
                }
                coroutineScope.launch {
                    rotationY.animateTo(event.values[1] * 180)
                }
                coroutineScope.launch {
                    rotationZ.animateTo(-event.values[2] * 180)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No op
            }
        }

//        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { sensor ->
//            sensorManager.registerListener(
//                rotationListener,
//                sensor,
//                SensorManager.SENSOR_DELAY_NORMAL
//            )
//        }


        // Step 6.2 v2 Rotation Shift
        var lastRotationReading = FloatArray(9)
        var lastRotationSet = false

        val smoothRotationListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (lastRotationSet) {
                    Log.d(
                        "Rotation values: ", "" +
                                "x: ${event.values[0]} " +
                                "y: ${event.values[1]} " +
                                "z: ${event.values[2]} "
                    )

                    val currentRotationReading = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(currentRotationReading, event.values)
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(currentRotationReading, orientationAngles)

//                    val orientationAngles = getOrientationGimbalLockCorrected(
//                        lastRotationReading = lastRotationReading,
//                        currentRotationReading = currentRotationReading
//                    )

                    // Smooth animation
                    val pitch = orientationAngles[1]
                    val roll = orientationAngles[2]

                    // Smooth Rotation
                    coroutineScope.launch {
                        rotationX.animateTo(mapRotation(pitch, HalfPi))
                    }
                    coroutineScope.launch {
                        rotationY.animateTo(mapRotation(roll))

                    }


                    // Smooth Translation
                    coroutineScope.launch {
                        val offsetX = mapTranslationWidth(roll, screenSize)
                        val offsetY = mapTranslationHeight(pitch, screenSize, HalfPi)

                        translation.animateTo(Offset(x = offsetX, y = -offsetY))
                    }

                } else {
                    lastRotationReading = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(lastRotationReading, event.values)
                    lastRotationSet = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No op
            }
        }

//        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)?.let { sensor ->
//            sensorManager.registerListener(
//                smoothRotationListener,
//                sensor,
//                SensorManager.SENSOR_DELAY_UI
//            )
//        }

        // Step 7.2 Shake it!
        val shakeStopDelay = 100
        var lastAcceleration = SensorManager.GRAVITY_EARTH
        var acceleration = 0f
        var isShaking = false
        var lastShakeTime: Long = 0

        val shakeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // source: https://stackoverflow.com/questions/2317428/how-to-refresh-app-upon-shaking-the-device
                val currentAcceleration = sqrt(x * x + y * y + z * z)
                val delta: Float = currentAcceleration - lastAcceleration
                acceleration = acceleration * 0.9f + delta

                if (acceleration > 8f) {
                    lastShakeTime = System.currentTimeMillis()
                    isShaking = true
                    // Shake started
                } else if (isShaking && System.currentTimeMillis() - lastShakeTime > shakeStopDelay) {
                    isShaking = false
                    // Shake ended
                    // Translation
                    coroutineScope.launch {
                        translation.animateTo(
                            targetValue = Offset.Zero,
                            animationSpec = keyframes {
                                durationMillis = 500
                                Offset.Zero atFraction 0f
                                Offset(x = screenSize.widthPx * 0.33f, y = 0f) atFraction 0.25f
                                Offset.Zero atFraction 0.5f
                                Offset(x = screenSize.widthPx * -0.33f, y = 0f) atFraction 0.75f
                                Offset.Zero atFraction 1f
                            }
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
                lastAcceleration = currentAcceleration
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No op
            }
        }

//        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let { sensor ->
//            sensorManager.registerListener(
//                shakeListener,
//                sensor,
//                SensorManager.SENSOR_DELAY_NORMAL
//            )
//        }

        // Step 8.2 Bonus
        val lightListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val lux = event.values[0]
                Log.d("Light values: ", "$lux")
                if (lux < 10f) {
                    // Dark
                    coroutineScope.launch {
                        fluffColor.animateTo(SheepColor.Black)
                    }
                    coroutineScope.launch {
                        glassesColor.animateTo(Color.White.copy(alpha = 0.5f))
                    }
                } else {
                    // Light
                    coroutineScope.launch {
                        fluffColor.animateTo(SheepColor.Green)
                    }
                    coroutineScope.launch {
                        glassesColor.animateTo(SheepColor.Black)
                    }
                }

            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No op
            }
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { sensor ->
            sensorManager.registerListener(
                lightListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        onPauseOrDispose {
            sensorManager.unregisterListener(rotationListener)
            sensorManager.unregisterListener(smoothRotationListener)
            sensorManager.unregisterListener(shakeListener)
            sensorManager.unregisterListener(lightListener)
        }
    }

    // 5.2.2 Fling
    fun doFlingMove(velocity: Velocity) {
        //1. Calculate target offset based on velocity
        val velocityOffset = Offset(velocity.x / 2f, velocity.y / 2f)

        val targetOffset = decay.calculateTargetValue(
            typeConverter = Offset.VectorConverter,
            initialValue = translation.value,
            initialVelocity = velocityOffset,
        )

        // 2. If the target offset is within bounds, animate to it
        if (targetOffset.x < screenSize.halfSize.x && targetOffset.x > -screenSize.halfSize.x &&
            targetOffset.y < screenSize.halfSize.y && targetOffset.y > -screenSize.halfSize.y
        ) {
            coroutineScope.launch {
                translation.animateDecay(velocityOffset, decay)
            }
        }
        // 3. If not, animate to farthest point within bounds and then animate back to center
        else {
            coroutineScope.launch {
                val adjustedOffset = Offset(
                    x = if (targetOffset.x < -screenSize.halfSize.x) -screenSize.halfSize.x else if (targetOffset.x > screenSize.halfSize.x) screenSize.halfSize.x else targetOffset.x,
                    y = if (targetOffset.y < -screenSize.halfSize.y) -screenSize.halfSize.y else if (targetOffset.y > screenSize.halfSize.y) screenSize.halfSize.y else targetOffset.y
                )
                translation.animateTo(adjustedOffset)

                translation.animateTo(
                    Offset(0f, 0f),
                    spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ComposableSheep(
            sheep = sheep,
            fluffColor = fluffColor.value,
            glassesColor = glassesColor.value,
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.Center)
                // Step 1.1.2 set scale in graphicsLayer(!)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.rotationX = rotationX.value
                    this.rotationY = rotationY.value
                    this.rotationZ = rotationZ.value
                    translationX = translation.value.x
                    translationY = translation.value.y

                }
                // Step 1.2 clickable scale change
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        coroutineScope.launch {
                            val newScale = if (scale.value == 1f) 1.2f else 1f
                            scale.animateTo(newScale)
                        }
                    })
                // Step 2.2 Double tap pointerInput || Step 3.2 Long press pointerInput
                .pointerInput(Unit) {
                    var isSpinning: Boolean = false
                    detectTapGestures(
                        onTap = {
                            coroutineScope.launch {
                                val newScale = if (scale.value == 1f) 1.2f else 1f
                                scale.animateTo(newScale)
                            }
                        },
                        onDoubleTap = {
                            coroutineScope.launch {
                                listOf(-45f, 0f, 45f, 0f).forEach {
                                    rotationZ.animateTo(
                                        targetValue = it,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMedium,
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                        )
                                    )
                                    delay(DefaultDelay)
                                }
                            }
                        },
                        onPress = {
                            awaitRelease()
                            if (isSpinning) {
                                isSpinning = false
                                rotationZ.animateTo(0f)
                            }
                        },
                        onLongPress = {
                            isSpinning = true
                            coroutineScope.launch {
                                rotationZ.animateTo(
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        keyframes {
                                            0f atFraction 0f
                                            360f atFraction 1f
                                        }
                                    )
                                )
                            }
                        }
                    )
                }
                // Step 4.2 Draggable2D || 5.2.1 Fling onDragStopped
                .draggable2D(
                    state = draggableState,
                    onDragStopped = { velocity ->
                        doFlingMove(velocity)
                    }
                )
        )
    }
}


