package dev.nstv.sheepit.gestures.ui.screen

import android.hardware.Sensor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.nstv.composablesheep.library.ComposableSheep
import dev.nstv.composablesheep.library.model.Sheep
import dev.nstv.sheepit.gestures.sensormanager.AndroidSensorManager
import dev.nstv.sheepit.gestures.sensormanager.rememberSensorManager
import dev.nstv.sheepit.gestures.util.AccelerationThreshold
import dev.nstv.sheepit.gestures.util.ScreenSize
import dev.nstv.sheepit.gestures.util.SensorMagnitude
import dev.nstv.sheepit.gestures.util.rememberScreenSize
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedSensorsScreen(
    sheep: Sheep,
    modifier: Modifier = Modifier,
    screenSize: ScreenSize = rememberScreenSize(),
    sensorManager: AndroidSensorManager = rememberSensorManager(),
) {

    val coroutineScope = rememberCoroutineScope()

    // Sheep Properties
    val sheepScale = remember { Animatable(1f) }
    val sheepRotation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val sheepTranslation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    sheepTranslation.updateBounds(
        lowerBound = Offset(-screenSize.widthPx / 2f, -screenSize.heightPx / 2f),
        upperBound = Offset(screenSize.widthPx / 2f, screenSize.heightPx / 2f),
    )

    // Gesture states
    val decay = rememberSplineBasedDecay<Offset>()
    var isDragging by remember { mutableStateOf(false) }
    val draggableState = rememberDraggable2DState { delta ->
        coroutineScope.launch {
            sheepTranslation.snapTo(sheepTranslation.value.plus(delta))
        }
    }

    LifecycleResumeEffect(sensorManager) {
        // Gyroscope
        sensorManager.registerListener(Sensor.TYPE_GYROSCOPE) { sensorEvent ->

            val xValue = sensorEvent.values[0]
            val yValue = sensorEvent.values[1]
            val zValue = sensorEvent.values[2]

            if (!isDragging && (abs(yValue) > AccelerationThreshold || abs(xValue) > AccelerationThreshold)) {

                println("GyroScopeEvent: $sensorEvent, values: ${sensorEvent.values.joinToString(",")}")

                val velocity = Offset(SensorMagnitude.times(yValue), SensorMagnitude.times(xValue))

                val decayOffset = decay.calculateTargetValue(
                    typeConverter = Offset.VectorConverter,
                    initialValue = sheepTranslation.value,
                    initialVelocity = velocity,
                )
                coroutineScope.launch {
                    sheepTranslation.animateTo(decayOffset, initialVelocity = velocity)
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

    Box(modifier = modifier.fillMaxSize()) {
        ComposableSheep(
            sheep = sheep,
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
//                .animateOrientationChange(
//                    adjusted = true,
//                )
                .graphicsLayer {
                    translationX = sheepTranslation.value.x
                    translationY = sheepTranslation.value.y
                    rotationX = sheepRotation.value.x
                    rotationY = sheepRotation.value.y
                    scaleX = sheepScale.value
                    scaleY = sheepScale.value
                }
                .pointerInput(PointerEventType.Press) {
                    awaitEachGesture {
                        awaitFirstDown()
                        coroutineScope.launch {
                            sheepScale.animateTo(1.2f)
                        }
                    }
                }
                .draggable2D(
                    state = draggableState,
                    onDragStarted = {
                        isDragging = true
                    },
                    onDragStopped = {
                        sheepScale.animateTo(1f)
                        isDragging = false
                    }
                )
        )
    }
}

