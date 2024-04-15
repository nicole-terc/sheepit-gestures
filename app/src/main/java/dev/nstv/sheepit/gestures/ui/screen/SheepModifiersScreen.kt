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
import dev.nstv.composablesheep.library.ComposableSheep
import dev.nstv.composablesheep.library.model.Sheep
import dev.nstv.sheepit.gestures.sensormanager.AndroidSensorManager
import dev.nstv.sheepit.gestures.sensormanager.modifiers.danceOrientationChange
import dev.nstv.sheepit.gestures.sensormanager.modifiers.danceTaps
import dev.nstv.sheepit.gestures.sensormanager.rememberSensorManager
import dev.nstv.sheepit.gestures.util.ScreenSize
import dev.nstv.sheepit.gestures.util.rememberScreenSize
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SheepModifiersScreen(
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
        lowerBound = Offset(-screenSize.widthPx / 2f, -screenSize.heightPx / 2f),
        upperBound = Offset(screenSize.widthPx / 2f, screenSize.heightPx / 2f),
    )

    // Gesture states
    val decay = rememberSplineBasedDecay<Offset>()
    var isDragging by remember { mutableStateOf(false) }
    val draggableState = rememberDraggable2DState { delta ->
        coroutineScope.launch {
            translation.snapTo(translation.value.plus(delta))
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
                .danceOrientationChange(adjusted = true)
                .danceTaps()
                .draggable2D(
                    state = draggableState,
                    onDragStarted = {
                        isDragging = true
                    },
                    onDragStopped = {
                        scale.animateTo(1f)
                        isDragging = false
                    }
                )
        )
    }
}


