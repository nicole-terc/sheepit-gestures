package dev.nstv.sheepit.gestures.sensormanager.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.nstv.sheepit.gestures.sensormanager.AndroidSensorManager
import dev.nstv.sheepit.gestures.sensormanager.CUSTOM_ORIENTATION
import dev.nstv.sheepit.gestures.sensormanager.CUSTOM_ORIENTATION_CORRECTED
import dev.nstv.sheepit.gestures.sensormanager.SensorType
import dev.nstv.sheepit.gestures.sensormanager.rememberSensorManager

// General sensorManager
@Composable
fun Modifier.onSensorEvent(
    sensors: List<SensorType>,
    sensorManager: AndroidSensorManager = rememberSensorManager(),
    onSensorEvent: (SensorType, values: FloatArray) -> Unit,
): Modifier {
    var enabled by remember { mutableStateOf(true) }
    LifecycleResumeEffect(sensorManager) {
        enabled = true
        onPauseOrDispose {
            sensorManager.unregisterAll()
            enabled = false
        }
    }

    return this then (if (enabled) {
        OnSensorEventElement(
            enabled = enabled,
            sensorManager = sensorManager,
            sensors = sensors,
            onSensorEvent = onSensorEvent,
        )
    } else {
        Modifier
    })
}

private data class OnSensorEventElement(
    val enabled: Boolean,
    val sensorManager: AndroidSensorManager,
    val sensors: List<SensorType>,
    val onSensorEvent: (SensorType, FloatArray) -> Unit,
) :
    ModifierNodeElement<OnSensorEventNode>() {
    override fun create(): OnSensorEventNode =
        OnSensorEventNode(sensorManager, sensors, onSensorEvent)

    override fun update(node: OnSensorEventNode) {
        node.sensors = sensors
        node.onSensorEvent = onSensorEvent
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OnSensorEventElement) return false

        if (sensors != other.sensors) return false
        if (onSensorEvent != other.onSensorEvent) return false
        if (sensorManager != other.sensorManager) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sensorManager.hashCode()
        result = 31 * result + sensors.hashCode()
        result = 31 * result + onSensorEvent.hashCode()
        return result
    }
}

// Node
private class OnSensorEventNode(
    var sensorManager: AndroidSensorManager,
    var sensors: List<SensorType>,
    var onSensorEvent: (SensorType, FloatArray) -> Unit,
) : Modifier.Node(), CompositionLocalConsumerModifierNode {

    override fun onAttach() {
        super.onAttach()
        sensors.forEach { sensorType ->
            when (sensorType) {
                CUSTOM_ORIENTATION -> {
                    sensorManager.observeOrientationChanges { orientation ->
                        onSensorEvent(sensorType, orientation.asFloatArray())
                    }
                }

                CUSTOM_ORIENTATION_CORRECTED -> {
                    sensorManager.observeOrientationChangesWithCorrection { orientation ->
                        onSensorEvent(sensorType, orientation.asFloatArray())
                    }
                }

                else -> {
                    sensorManager.registerListener(
                        sensorType = sensorType,
                        onSensorChanged = { event ->
                            onSensorEvent(sensorType, event.values)
                        },
                    )
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        sensorManager.unregisterAll()
    }
}