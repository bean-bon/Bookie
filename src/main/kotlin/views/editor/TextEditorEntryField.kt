package views.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import views.viewmodels.TextEditorEntryFieldModel

@Composable
fun TextEditorEntryField(
    model: TextEditorEntryFieldModel
) {
    Box(Modifier.fillMaxSize().padding(10.dp)) {
        BasicTextField(
            value = model.textBoxContent,
            onValueChange = { new -> model.textBoxContent = new; model.modified = true },
        )
        if (model.textBoxContent.isBlank()) {
            Text("Type here to get started...", color = Color.Black, fontSize = 12.sp)
        }
    }
}
