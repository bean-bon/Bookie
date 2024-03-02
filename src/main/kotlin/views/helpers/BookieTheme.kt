package views.helpers

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BookieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme (
        colors = if (darkTheme) darkColors(surface = Color.LightGray, onSurface = Color.Black) else lightColors(surface = Color.LightGray),
        content = content
    )
}