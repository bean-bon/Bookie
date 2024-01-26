package views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun FileTree() {
    // File navigation tree.
    Column(
        modifier = Modifier
            .fillMaxHeight(1f)
            .fillMaxWidth()
            .background(Color.Cyan)
    ) {
        Text("File Navigation")
    }
}