package dev.nstv.sheepit.gestures.sensormanager.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import dev.nstv.sheepit.gestures.sensormanager.AndroidSensorManager
import dev.nstv.sheepit.gestures.sensormanager.CUSTOM_ORIENTATION
import dev.nstv.sheepit.gestures.sensormanager.CUSTOM_ORIENTATION_CORRECTED
import dev.nstv.sheepit.gestures.sensormanager.DeviceOrientation
import dev.nstv.sheepit.gestures.sensormanager.rememberSensorManager
import dev.nstv.sheepit.gestures.util.HalfPi
import dev.nstv.sheepit.gestures.util.ScreenSize
import dev.nstv.sheepit.gestures.util.mapRotation
import dev.nstv.sheepit.gestures.util.mapTranslationHeight
import dev.nstv.sheepit.gestures.util.mapTranslationWidth
import dev.nstv.sheepit.gestures.util.rememberScreenSize
import kotlinx.coroutines.launch


/**
 * Dance Orientation Change Modifier
 */
@Composable
fun Modifier.danceOrientationChange(
    enabled: Boolean = true,
    adjusted: Boolean = false,
    onOrientationChanged: ((DeviceOrientation) -> Unit)? = null,
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val sensorManager: AndroidSensorManager = rememberSensorManager()
    val screenSize: ScreenSize = rememberScreenSize()

    val rotation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val translation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val sensors =
        remember(adjusted) { listOf(if (adjusted) CUSTOM_ORIENTATION_CORRECTED else CUSTOM_ORIENTATION) }

    val sensorModifier = if (enabled) {
        onSensorEvent(
            sensors = sensors,
            sensorManager = sensorManager,
        ) { _, values ->
            val roll = values[2]
            val pitch = values[1]

            // Rotation
            coroutineScope.launch {
                val degreesX = mapRotation(pitch, HalfPi)
                val degreesY = mapRotation(roll)
                rotation.animateTo(Offset(x = degreesX, y = degreesY))
            }

            // Styled movement
            coroutineScope.launch {
                val offsetX = mapTranslationWidth(roll, screenSize)
                val offsetY = mapTranslationHeight(pitch, screenSize, HalfPi)
                translation.animateTo(Offset(x = offsetX, y = -offsetY))
            }
            onOrientationChanged?.invoke(DeviceOrientation(values))
        }.then(DanceMoveElement {
            translationX = translation.value.x
            translationY = translation.value.y
            rotationX = rotation.value.x
            rotationY = rotation.value.y
        })
    } else Modifier

    return this then sensorModifier
}


