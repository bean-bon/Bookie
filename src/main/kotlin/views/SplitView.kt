package views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

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

    BoxWithConstraints {
        Row {
            // Display left side view.
            Column(Modifier.fillMaxWidth(leftWidth)) {
                leftView()
            }
            // Splitter.
            Column(
                Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(Color.Magenta)
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
            ) {}
            // Display right side view.
            Column {
                rightView()
            }
        }
    }

}