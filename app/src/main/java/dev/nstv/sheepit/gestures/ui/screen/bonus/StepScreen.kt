package dev.nstv.sheepit.gestures.ui.screen.bonus

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.nstv.composablesheep.library.ComposableSheep
import dev.nstv.composablesheep.library.model.Sheep
import dev.nstv.composablesheep.library.util.SheepColor
import dev.nstv.sheepit.gestures.util.PermissionsWrapper
import dev.nstv.sheepit.gestures.util.sideToSideKeyframes
import kotlinx.coroutines.launch

@Composable
fun StepScreen(
    modifier: Modifier = Modifier,
    sheep: Sheep = Sheep(fluffColor = SheepColor.Orange),
) = PermissionsWrapper {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    var steps by remember { mutableIntStateOf(0) }
    val rotationZ = remember { Animatable(0f) }

    LifecycleResumeEffect(Unit) {
        val stepCounterListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                steps = event.values[0].toInt()
                Log.d("StepScreen", "New steps: $steps")
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No op
            }
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sensorManager.registerListener(
                stepCounterListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        val stepDetectorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                coroutineScope.launch {
                    rotationZ.animateTo(
                        targetValue = 0f,
                        animationSpec = sideToSideKeyframes(0f, 15f, duration = 300)
                    )
                }

            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No op
            }
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let {
            sensorManager.registerListener(
                stepDetectorListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        onPauseOrDispose {
//            sensorManager.unregisterListener(stepCounterListener)
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AnimatedContent(
            targetState = steps,
            label = "Step counter",
        ) { newSteps ->
            Text(
                text = "Steps: $newSteps",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxSize(),
            )
        }

        ComposableSheep(
            sheep = sheep,
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    this.rotationZ = rotationZ.value
                },
        )
    }
}