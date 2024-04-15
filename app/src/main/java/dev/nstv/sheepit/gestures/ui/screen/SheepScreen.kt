package dev.nstv.sheepit.gestures.ui.screen

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.nstv.composablesheep.library.ComposableSheep
import dev.nstv.composablesheep.library.model.Sheep
import dev.nstv.sheepit.gestures.sensormanager.AndroidSensorManager
import dev.nstv.sheepit.gestures.sensormanager.modifiers.animateOrientationChange
import dev.nstv.sheepit.gestures.sensormanager.modifiers.danceFling
import dev.nstv.sheepit.gestures.sensormanager.modifiers.danceTaps
import dev.nstv.sheepit.gestures.sensormanager.rememberSensorManager
import dev.nstv.sheepit.gestures.util.DefaultDelay
import dev.nstv.sheepit.gestures.util.ScreenSize
import dev.nstv.sheepit.gestures.util.defaultDanceAnimationSpec
import dev.nstv.sheepit.gestures.util.rememberScreenSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SheepScreen(
    sheep: Sheep,
    modifier: Modifier = Modifier,
    screenSize: ScreenSize = rememberScreenSize(),
    sensorManager: AndroidSensorManager = rememberSensorManager(),
) {

    val coroutineScope = rememberCoroutineScope()

    // Sheep Properties
    val color = remember { Animatable(sheep.fluffColor) }
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val rotationZ = remember { Animatable(0f) }
    val translation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    translation.updateBounds(
        lowerBound = Offset(-screenSize.middlePointPx.x, -screenSize.middlePointPx.y),
        upperBound = Offset(screenSize.middlePointPx.x, screenSize.middlePointPx.y),
    )

    // Gesture states
    val decay = rememberSplineBasedDecay<Offset>()
    var isDragging by remember { mutableStateOf(false) }
    val draggableState = rememberDraggable2DState { delta ->
        coroutineScope.launch {
            translation.snapTo(translation.value.plus(delta))
        }
    }

    LifecycleResumeEffect(sensorManager) {
        /*/ Gyroscope
        sensorManager.registerListener(Sensor.TYPE_GYROSCOPE) { sensorEvent ->

            val xValue = sensorEvent.values[0]
            val yValue = sensorEvent.values[1]
            val zValue = sensorEvent.values[2]

            if (!isDragging && (abs(yValue) > AccelerationThreshold || abs(xValue) > AccelerationThreshold)) {

                println("GyroScopeEvent: $sensorEvent, values: ${sensorEvent.values.joinToString(",")}")

                val velocity = Offset(SensorMagnitude.times(yValue), SensorMagnitude.times(xValue))

                val decayOffset = decay.calculateTargetValue(
                    typeConverter = Offset.VectorConverter,
                    initialValue = translation.value,
                    initialVelocity = velocity,
                )
                coroutineScope.launch {
                    translation.animateTo(decayOffset, initialVelocity = velocity)
                }
            }
        }
        // */

        /// Orientation
//        sensorManager.observeOrientationChangesWithCorrection { orientation ->
//            val roll = orientation.roll
//            val pitch = orientation.pitch
//
//            coroutineScope.launch {
//                // Rotation
//                val degreesX =
////                    orientation.pitchDegrees
//                    mapValues(
//                        value = pitch,
//                        fromStart = -PI.toFloat() / 2,
//                        fromEnd = PI.toFloat() / 2,
//                        toStart = -90f,
//                        toEnd = 90f,
//                    )
//
//                val degreesY =
////                    orientation.rollDegrees
//                    mapValues(
//                        value = roll,
//                        fromStart = -PI.toFloat(),
//                        fromEnd = PI.toFloat(),
//                        toStart = -90f,
//                        toEnd = 90f,
//                    )
//
//                sheepRotation.animateTo(
//                    Offset(
//                        x = degreesX,
//                        y = degreesY,
//                    ),
//                )
//            }
//            coroutineScope.launch {
//                // Styled movement
//                val offsetX = mapValues(
//                    value = roll,
//                    fromStart = -PI.toFloat(),
//                    fromEnd = PI.toFloat(),
//                    toStart = -screenSize.widthPx / 2f,
//                    toEnd = screenSize.widthPx / 2f,
//                )
//
//                val offsetY = mapValues(
//                    value = pitch,
//                    fromStart = -PI.toFloat() / 2,
//                    fromEnd = PI.toFloat() / 2,
//                    toStart = -screenSize.heightPx / 2f,
//                    toEnd = screenSize.heightPx / 2f,
//                )
//
//                sheepTranslation.animateTo(
//                    Offset(
//                        x = offsetX,
//                        y = -offsetY,
//                    )
//                )
//            }
//        }
        // */
        // Gestures


        onPauseOrDispose {
            sensorManager.unregisterAll()
        }
    }

    fun doTapMove() {
        coroutineScope.launch {
            rotationZ.animateTo(-45f, defaultDanceAnimationSpec())
            delay(DefaultDelay)
            rotationZ.animateTo(0f, defaultDanceAnimationSpec())
            delay(DefaultDelay)
            rotationZ.animateTo(45f, defaultDanceAnimationSpec())
            delay(DefaultDelay)
            rotationZ.animateTo(0f, defaultDanceAnimationSpec())
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ComposableSheep(
            sheep = sheep,
            fluffColor = color.value,
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = translation.value.x
                    translationY = translation.value.y
                    rotationX = rotation.value.x
                    rotationY = rotation.value.y
                    this.rotationZ = rotationZ.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
//                .draggable2D(
//                    state = draggableState,
//                    onDragStarted = {
//                        isDragging = true
//                    },
//                    onDragStopped = {
//                        scale.animateTo(1f)
//                        isDragging = false
//                    }
//                )
//                .pointerInput(PointerEventType.Press) {
//                    detectTapGestures(
//                        onPress = {
//                            scale.animateTo(1.2f)
//                            awaitRelease()
//                            scale.animateTo(1f)
//                        },
//                        onTap = {
//                            coroutineScope.launch {
//                                color.animateTo(SheepColor.random(color.value))
//                            }
//                        },
//                        onDoubleTap = {
//                            doTapMove()
//                        },
//                    )
//                }
                .animateOrientationChange(adjusted = true)
                .danceFling()
                .danceTaps()
        )
    }
}


