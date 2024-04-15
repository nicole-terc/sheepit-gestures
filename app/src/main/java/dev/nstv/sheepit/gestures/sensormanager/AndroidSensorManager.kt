package dev.nstv.sheepit.gestures.sensormanager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.nstv.sheepit.gestures.util.HalfPi
import dev.nstv.sheepit.gestures.util.Pi
import dev.nstv.sheepit.gestures.util.TwoPi


typealias SensorType = Int

const val ShowSensorLog = false
const val CUSTOM_ORIENTATION = -10
const val CUSTOM_ORIENTATION_CORRECTED = -11

class AndroidSensorManager(
    private val context: Context
) {

    private val sensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val listeners: MutableMap<Int, SensorEventListener> = mutableMapOf()

    // Orientation
    var accelerometerReading: FloatArray? = null
    var magnetometerReading: FloatArray? = null
    var lastRotationReading: FloatArray = FloatArray(9)
    var lastRotationSet = false

    fun getSensorList(sensorType: SensorType): List<Sensor> {
        return sensorManager.getSensorList(sensorType)
    }

    fun getDefaultSensor(sensorType: SensorType): Sensor? {
        return sensorManager.getDefaultSensor(sensorType)
    }


    fun registerListener(
        sensorType: SensorType,
        samplingPeriod: Int = SensorManager.SENSOR_DELAY_NORMAL,
        onAccuracyChanged: (SensorType?, Int) -> Unit = { _, _ -> },
        onSensorChanged: (SensorEvent) -> Unit
    ) {

        if (listeners.containsKey(sensorType)) {
            sensorManager.unregisterListener(listeners[sensorType])
        }

        sensorManager.getDefaultSensor(sensorType)?.let { sensor ->
            val sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    onSensorChanged(event)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    onAccuracyChanged(sensor?.type, accuracy)
                }
            }

            listeners[sensorType] = sensorEventListener
            sensorManager.registerListener(
                sensorEventListener,
                sensor,
                samplingPeriod
            )
        }
    }

    fun unregisterListener(sensorType: SensorType) {
        if (ShowSensorLog) {
            println("unregistering listener for sensor type: $sensorType")
        }
        listeners[sensorType]?.let { sensorManager.unregisterListener(it) }
    }

    fun unregisterAll() {
        listeners.forEach { (_, listener) -> sensorManager.unregisterListener(listener) }
        listeners.clear()
        accelerometerReading = null
        magnetometerReading = null
        lastRotationReading = FloatArray(9)
        lastRotationSet = false
    }

    fun observeOrientationChanges(
        onOrientationChanged: (DeviceOrientation) -> Unit
    ) {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (ShowSensorLog) {
                    println("event received: ${event.sensor.type}")
                }
                if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                    accelerometerReading = event.values
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    magnetometerReading = event.values
                }

                if (accelerometerReading != null && magnetometerReading != null) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrix(
                        rotationMatrix,
                        null,
                        accelerometerReading,
                        magnetometerReading
                    )

                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val orientation = DeviceOrientation(
                        orientationAngles[0],
                        orientationAngles[1],
                        orientationAngles[2],
                    )
                    if (ShowSensorLog) orientation.prettyPrint()
                    onOrientationChanged(orientation)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Do nothing
            }
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        listeners[CUSTOM_ORIENTATION] = sensorEventListener
    }

    // Corrected orientation to avoid Gimbal Lock
    fun observeOrientationChangesWithCorrection(onOrientationChanged: (DeviceOrientation) -> Unit) {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (lastRotationSet) {
                    val currentRotationReading = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(currentRotationReading, event.values)
                    val orientationAngles = FloatArray(3) { index ->
                        when (index) {
                            0 -> (lastRotationReading[0] * currentRotationReading[1] + lastRotationReading[3] * currentRotationReading[4] + lastRotationReading[6] * currentRotationReading[7]) * TwoPi
                            1 -> -(lastRotationReading[2] * currentRotationReading[1] + lastRotationReading[5] * currentRotationReading[4] + lastRotationReading[8] * currentRotationReading[7]) * HalfPi
                            2 -> -(lastRotationReading[2] * currentRotationReading[0] + lastRotationReading[5] * currentRotationReading[3] + lastRotationReading[8] * currentRotationReading[6]) * Pi
                            else -> 0f
                        }
                    }
                    val orientation = DeviceOrientation(orientationAngles)
                    if (ShowSensorLog) orientation.prettyPrint()
                    onOrientationChanged(orientation)
                } else {
                    lastRotationReading = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(lastRotationReading, event.values)
                    lastRotationSet = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Do nothing
            }
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        listeners[CUSTOM_ORIENTATION_CORRECTED] = sensorEventListener
    }
}

@Composable
fun rememberSensorManager(): AndroidSensorManager {
    val context = LocalContext.current
    return remember(context) {
        AndroidSensorManager(context)
    }
}
