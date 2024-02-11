package views.editor

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import views.viewmodels.TextEditorEntryFieldModel

@Composable
fun TextEditorEntryField(
    model: TextEditorEntryFieldModel
) {
    BasicTextField(
        value = model.textBoxContent,
        modifier = Modifier.padding(10.dp),
        onValueChange = { new -> model.textBoxContent = new; model.modified = true }
    )
}
