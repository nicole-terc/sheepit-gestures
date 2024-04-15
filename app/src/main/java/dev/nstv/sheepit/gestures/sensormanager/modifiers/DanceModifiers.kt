package dev.nstv.sheepit.gestures.sensormanager.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import dev.nstv.sheepit.gestures.util.DefaultDelay
import dev.nstv.sheepit.gestures.util.defaultDanceAnimationSpec
import dev.nstv.sheepit.gestures.util.rememberScreenSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Modifier.danceTaps(): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val rotationZ = remember { Animatable(0f) }
    var goLeft = true

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
        coroutineScope.launch {
            rotationZ.animateTo(
                360f,
                infiniteRepeatable(
                    keyframes {
                        durationMillis = 300
                        0f atFraction 0f
                        180f atFraction 0.5f
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
                rotationZ.animateTo(0f)
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

        if (decayOffset.x < screenSize.middlePointPx.x && decayOffset.x > -screenSize.middlePointPx.x &&
            decayOffset.y < screenSize.middlePointPx.y && decayOffset.y > -screenSize.middlePointPx.y
        ) {
            coroutineScope.launch {
                translation.animateDecay(velocity, decay)
            }
        } else {
            coroutineScope.launch {
                val adjustedOffset = Offset(
                    x = if (decayOffset.x < -screenSize.middlePointPx.x) -screenSize.middlePointPx.x else if (decayOffset.x > screenSize.middlePointPx.x) screenSize.middlePointPx.x else decayOffset.x,
                    y = if (decayOffset.y < -screenSize.middlePointPx.y) -screenSize.middlePointPx.y else if (decayOffset.y > screenSize.middlePointPx.y) screenSize.middlePointPx.y else decayOffset.y
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

// GraphicLayer handler

private data class DanceMoveElement(
    val block: GraphicsLayerScope.() -> Unit
) : ModifierNodeElement<DanceMoveModifier>() {
    override fun create() = DanceMoveModifier(block)

    override fun update(node: DanceMoveModifier) {
        node.layerBlock = block
    }
}

private class DanceMoveModifier(
    var layerBlock: GraphicsLayerScope.() -> Unit,
) : DelegatingNode() {
    init {
        delegate(
            DanceGraphicsModifier(layerBlock = layerBlock)
        )
    }
}

private class DanceGraphicsModifier(
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