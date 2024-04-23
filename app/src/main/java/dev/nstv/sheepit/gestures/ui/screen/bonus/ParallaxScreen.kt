package dev.nstv.sheepit.gestures.ui.screen.bonus

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.nstv.sheepit.gestures.R
import dev.nstv.sheepit.gestures.sensormanager.DeviceOrientation
import dev.nstv.sheepit.gestures.util.getOrientationGimbalLockCorrected

@Composable
fun ParallaxScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
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
        Image(
            painter = painterResource(id = R.drawable.nasa),
            contentDescription = "nasa photo",
            contentScale = ContentScale.None,
            modifier = Modifier.fillMaxSize(),
            alignment = BiasAlignment(
                horizontalBias = orientation.roll * 0.05f,
                verticalBias = -orientation.pitch * 0.05f,
            )
        )
    }
}