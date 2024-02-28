package views.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Composable for splitting two views with a movable divider.
 * @param defaultProportion the default proportion of the window the left view should take up.
 * @param minProportion the smallest proportion of the window the left view should take up.
 * @param maxProportion the largest proportion of the window the left view should take up.
 * @param leftView the left side Composable.
 * @param rightView the right side Composable.
 */
@Composable
fun SplitView(
    defaultProportion: Float = 0.3f,
    minProportion: Float = 0.1f,
    maxProportion: Float = 0.9f,
    leftView: @Composable () -> Unit,
    rightView: @Composable () -> Unit
) {

    var leftWidth by remember { mutableStateOf(defaultProportion) }
    val localDensity = LocalDensity.current

    val hoveredSplitter = remember { MutableInteractionSource() }
    val isHovered = hoveredSplitter.collectIsHoveredAsState()

    BoxWithConstraints {
        Row {
            // Display left side view.
            Column(Modifier.fillMaxWidth(leftWidth)) {
                leftView()
            }
            // Splitter.
            Box(Modifier.width(10.dp).fillMaxHeight()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                ) {
                    this@Row.AnimatedVisibility(
                        isHovered.value,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(Modifier.fillMaxSize().background(Color.Gray.copy(alpha = if (isHovered.value) 0.3f else 0f))) {}
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .background(MaterialTheme.colors.primaryVariant)
                        .hoverable(hoveredSplitter)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                val windowWidth = this@BoxWithConstraints.maxWidth.value
                                val deltaRatio = localDensity.density
                                leftWidth += delta / deltaRatio / windowWidth
                                if (leftWidth < minProportion) {
                                    leftWidth = minProportion
                                } else if (leftWidth > maxProportion) {
                                    leftWidth = maxProportion
                                }
                            }
                        )
                )
            }
            // Display right side view.
            Column {
                rightView()
            }
        }
    }

}