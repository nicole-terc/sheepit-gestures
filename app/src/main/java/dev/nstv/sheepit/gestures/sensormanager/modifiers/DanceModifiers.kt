package dev.nstv.sheepit.gestures.sensormanager.modifiers

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastAny
import dev.nstv.sheepit.gestures.util.DefaultDelay
import dev.nstv.sheepit.gestures.util.defaultDanceAnimationSpec
import dev.nstv.sheepit.gestures.util.rememberScreenSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

/**
 * Dance Taps Modifier
 */

@Composable
fun Modifier.danceTaps(): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val rotationZ = remember { Animatable(0f) }
    var goLeft = true
    var isSpinning = false

    fun doTapMove() {
        coroutineScope.launch {
            val angle = if (goLeft) -45f else 45f
            goLeft = !goLeft
            listOf(angle, 0f).forEach {
                rotationZ.animateTo(it, defaultDanceAnimationSpec())
                delay(DefaultDelay)
            }
        }
    }

    fun doDoubleTapMove() {
        coroutineScope.launch {
            listOf(-45f, 0f, 45f, 0f).forEach {
                rotationZ.animateTo(it, defaultDanceAnimationSpec())
                delay(DefaultDelay)
            }
        }
    }

    fun doLongPress() {
        isSpinning = true
        coroutineScope.launch {
            rotationZ.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    keyframes {
                        durationMillis = 300
                        0f atFraction 0f
                        360f atFraction 1f
                    }
                )
            )
        }
    }

    return this then DanceMoveElement {
        this.rotationZ = rotationZ.value
        this.scaleX = scale.value
        this.scaleY = scale.value
    }.pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                scale.animateTo(1.1f)
                awaitRelease()
                scale.animateTo(1f)
                if (isSpinning) {
                    rotationZ.animateTo(0f)
                    isSpinning = false
                }
            },
            onTap = {
                doTapMove()
            },
            onDoubleTap = {
                doDoubleTapMove()
            },
            onLongPress = {
                doLongPress()
            }
        )
    }
}

/**
 * Dance Fling Modifier
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.danceFling(): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val screenSize = rememberScreenSize()
    val translation = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val decay = rememberSplineBasedDecay<Offset>()
    val draggableState = rememberDraggable2DState { delta ->
        coroutineScope.launch {
            translation.snapTo(translation.value.plus(delta))
        }
    }

    fun doFling(velocity: Offset) {
        val decayOffset = decay.calculateTargetValue(
            typeConverter = Offset.VectorConverter,
            initialValue = translation.value,
            initialVelocity = velocity,
        )

        if (decayOffset.x < screenSize.halfSize.x && decayOffset.x > -screenSize.halfSize.x &&
            decayOffset.y < screenSize.halfSize.y && decayOffset.y > -screenSize.halfSize.y
        ) {
            coroutineScope.launch {
                translation.animateDecay(velocity, decay)
            }
        } else {
            coroutineScope.launch {
                val adjustedOffset = Offset(
                    x = if (decayOffset.x < -screenSize.halfSize.x) -screenSize.halfSize.x else if (decayOffset.x > screenSize.halfSize.x) screenSize.halfSize.x else decayOffset.x,
                    y = if (decayOffset.y < -screenSize.halfSize.y) -screenSize.halfSize.y else if (decayOffset.y > screenSize.halfSize.y) screenSize.halfSize.y else decayOffset.y
                )
                translation.animateTo(adjustedOffset)

                translation.animateTo(
                    Offset(0f, 0f),
                    spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    ),
                    initialVelocity = velocity
                )
            }
        }
    }

    return this then DanceMoveElement {
        translationX = translation.value.x
        translationY = translation.value.y
    }.draggable2D(
        state = draggableState,
        onDragStopped = { velocity ->
            doFling(Offset(velocity.x, velocity.y))
        }
    )
}

/**
 * Dance Resize Modifier
 */
@Composable
fun Modifier.danceResize(): Modifier {
    var scale by remember { mutableFloatStateOf(1f) }
    var rotationZ by remember { mutableFloatStateOf(0f) }


    return this then DanceMoveElement {
        this.rotationZ = rotationZ
        this.scaleX = scale
        this.scaleY = scale
    }.pointerInput(Unit) {
        //  Blocks dragging as it consumes all events
//        detectTransformGestures { centroid, pan, zoom, rotation ->
//            scale *= zoom
//            rotationZ *= rotation
//        }
        // copy & pasted from detectTransformGestures + modifications
        awaitEachGesture {
            val panZoomLock = false
            var rotation = 0f
            var zoom = 1f
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop
            var lockedToPanZoom = false

            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.fastAny { it.isConsumed }
                if (!canceled) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        rotation += rotationChange

                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)

                        if (zoomMotion > touchSlop ||
                            rotationMotion > touchSlop
                        ) {
                            pastTouchSlop = true
                            lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                        }
                    }

                    if (pastTouchSlop) {
                        val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                        if (effectiveRotation != 0f ||
                            zoomChange != 1f
                        ) {
                            //Gesture
                            scale *= zoomChange
                            rotationZ += rotationChange

                        }
                    }
                }
            } while (!canceled && event.changes.fastAny { it.pressed })
        }
    }
}

/**
 * Shared Graphics Element
 */

internal data class DanceMoveElement(
    val block: GraphicsLayerScope.() -> Unit
) : ModifierNodeElement<DanceMoveModifier>() {
    override fun create(): DanceMoveModifier {
        Log.d("HERE", "create NODE")
        return DanceMoveModifier(block)
    }

    override fun update(node: DanceMoveModifier) {
        Log.d("HERE", "update NODE")
        node.layerBlock = block
    }
}

internal class DanceMoveModifier(
    var layerBlock: GraphicsLayerScope.() -> Unit,
) : DelegatingNode() {
    init {
        delegate(DanceGraphicsModifier(layerBlock = layerBlock))
    }
}

internal class DanceGraphicsModifier(
    var layerBlock: GraphicsLayerScope.() -> Unit,
) : Modifier.Node(), LayoutModifierNode, DelegatableNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }
}