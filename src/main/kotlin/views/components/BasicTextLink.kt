package views.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Divider
import androidx.compose.material.Shapes
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BasicTextLink(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 20.sp,
    height: Dp = 20.dp,
    imageDescription: String? = null,
    imageResourcePath: String? = null,
    textPaddingValues: PaddingValues = PaddingValues(),
    onClick: () -> Unit
) {
    Row(
        modifier.clickable(onClick = onClick).height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageResourcePath != null) {
            Image(
                painterResource(imageResourcePath),
                modifier = Modifier.fillMaxHeight(),
                contentScale = ContentScale.Fit,
                contentDescription = imageDescription
            )
        }
        Text(
            modifier = Modifier.height(height).padding(textPaddingValues).align(Alignment.CenterVertically),
            text = text,
            fontSize = fontSize
        )
    }
}