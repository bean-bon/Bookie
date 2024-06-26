package views.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import backend.html.helpers.PathResolver
import views.viewmodels.TextEditorEntryFieldModel

@Composable
fun TextEditorEntryField(
    model: TextEditorEntryFieldModel,
) {

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(model.file) {
        focusRequester.requestFocus()
    }

    Box(Modifier
        .fillMaxSize()
        .padding(10.dp)
        .focusable(true)
        .semantics {
            this.contentDescription = "Text entry field for ${PathResolver.getRelativeFilePath(model.file)}"
        }
    ) {
        BasicTextField(
            value = model.textBoxContent,
            onValueChange = { new -> model.textBoxContent = new; model.modified = true },
            modifier = Modifier.background(MaterialTheme.colors.background).focusRequester(focusRequester),
            textStyle = TextStyle(color = MaterialTheme.colors.onBackground),
            cursorBrush = Brush.linearGradient(listOf(MaterialTheme.colors.onBackground, MaterialTheme.colors.onBackground))
        )
        if (model.textBoxContent.isBlank()) {
            Text(
                "Type here to get started...",
                color = MaterialTheme.colors.onBackground,
                fontSize = 12.sp
            )
        }
    }
}
