package dev.nstv.sheepit.gestures.ui.screen

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
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

    /**
     * Properties to animate
     */

    // Step 1.1.1 scale

    // Step 2.1 rotationZ

    // Step 6.1.1 rotationX and rotationY

    // Step 4.1 translation offset

    // Step 8.1 Bonus

    /**
     * Dragging State & Decay
     */
    // Step 4.2.1 Dragging state - Snap!

    // Step 5.2.3 Decay

    /**
     * Sensors
     */
    // Step 6.1.2

    LifecycleResumeEffect() {

        // Step 6.2 Rotation

        // Step 6.2 v2 Rotation Shift

        // Step 7.2 Shake it!

        // Step 8.2 Bonus

        onPauseOrDispose {
        }
    }

    // 5.2.2 Fling
    fun doFlingMove(velocity: Velocity) {
        //1. Calculate target offset based on velocity

        // 2. If the target offset is within bounds, animate to it

        // 3. If not, animate to farthest point within bounds and then animate back to center

    }

    Box(modifier = modifier.fillMaxSize()) {
        ComposableSheep(
            sheep = sheep,
            // 8.1 Add Bonus Fluff & Glasses Color
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.Center)
                // Step 1.1.2 set scale in graphicsLayer(!)

                // Step 1.2 clickable scale change

                // Step 2.2 Double tap pointerInput || Step 3.2 Long press pointerInput

                // Step 4.2.2 Draggable2D || 5.2.1 Fling onDragStopped

        )
    }
}


