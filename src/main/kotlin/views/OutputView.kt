package views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun BookieMDOutput(
    html: String
) {
    Column(Modifier.fillMaxSize()) {
        Text(html)
    }

}