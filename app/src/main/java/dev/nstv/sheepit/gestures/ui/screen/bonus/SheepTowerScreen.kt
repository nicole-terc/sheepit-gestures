package dev.nstv.sheepit.gestures.ui.screen.bonus

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.nstv.composablesheep.library.ComposableSheep
import dev.nstv.composablesheep.library.util.SheepColor
import dev.nstv.sheepit.gestures.sensormanager.DeviceOrientation
import dev.nstv.sheepit.gestures.util.ShadowSheep
import dev.nstv.sheepit.gestures.util.getOrientationGimbalLockCorrected

@Composable
fun SheepTowerScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val sheepSizes = listOf(400.dp, 300.dp, 200.dp, 100.dp)
    val sheepColors =
        listOf(SheepColor.Purple, SheepColor.Green, SheepColor.Blue, SheepColor.Orange)
    var orientation by remember { mutableStateOf(DeviceOrientation(0f, 0f, 0f)) }



    LifecycleResumeEffect(Unit) {
        var lastRotationReading = FloatArray(9)
        var lastRotationSet = false
        val orientationListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                Log.d(
                    "Rotation values: ", "" +
                            "x: ${event.values[0]} " +
                            "y: ${event.values[1]} " +
                            "z: ${event.values[2]} "
                )

                if (lastRotationSet) {
                    val currentRotationReading = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(currentRotationReading, event.values)
                    val orientationAngles = getOrientationGimbalLockCorrected(
                        lastRotationReading = lastRotationReading,
                        currentRotationReading = currentRotationReading
                    )

                    orientation = DeviceOrientation(orientationAngles)

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

        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)?.let {
            sensorManager.registerListener(
                orientationListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        onPauseOrDispose {
            sensorManager.unregisterListener(orientationListener)
        }
    }
    Box(modifier.fillMaxSize()) {
        sheepSizes.forEachIndexed { index, size ->
            // border
            ComposableSheep(
                sheep = ShadowSheep,
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = orientation.roll * (index + 1) * 18f
                        translationY = -orientation.pitch * (index + 1) * 28f
                    },

                )
            // sheep
            ComposableSheep(
                fluffColor = sheepColors[index],
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = orientation.roll * (index + 1) * 20f
                        translationY = -orientation.pitch * (index + 1) * 30f
                    },

                )
        }
    }
}