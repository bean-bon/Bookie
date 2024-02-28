package views.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import views.viewmodels.TextEditorEntryFieldModel

@Composable
fun TextEditorEntryField(
    model: TextEditorEntryFieldModel,
) {
    Box(Modifier.fillMaxSize().padding(10.dp)) {
        BasicTextField(
            value = model.textBoxContent,
            onValueChange = { new -> model.textBoxContent = new; model.modified = true },
            modifier = Modifier.background(MaterialTheme.colors.background),
            textStyle = TextStyle(color = MaterialTheme.colors.onBackground)
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
