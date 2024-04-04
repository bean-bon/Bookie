package views.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import backend.html.helpers.PathResolver
import views.viewmodels.TextEditorEntryFieldModel
import kotlin.io.path.name

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextEditorTab(
    model: TextEditorEntryFieldModel,
    selected: Boolean,
    onSelect: (TextEditorEntryFieldModel) -> Unit,
    onClose: (TextEditorEntryFieldModel) -> Unit
) {
    TooltipArea(
        tooltip = {
            Text(
                PathResolver.getRelativeFilePath(model.file).toString(),
                Modifier.padding(2.dp).background(MaterialTheme.colors.surface),
                color = MaterialTheme.colors.onSurface,
                fontSize = 12.sp
            )
        },
        tooltipPlacement = TooltipPlacement.ComponentRect(Alignment.BottomCenter),
        delayMillis = 1000
    ) {
        val backgroundColour =
            if (selected) MaterialTheme.colors.primary
            else MaterialTheme.colors.secondary
        Column {
            Row(
                modifier = Modifier
                    .clickable { onSelect(model) }
                    .background(backgroundColour),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    model.file.name + if (model.modified) "*" else "",
                    modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                )
                BasicText(
                    "X",
                    modifier = Modifier
                        .clickable { onClose(model) }
                        .padding(5.dp)
                        .semantics {
                            this.text = AnnotatedString("Close editor tab for ${model.file.name}")
                            this.role = Role.Button
                            this.selected = selected
                        }
                )
            }
            if (selected) {
                Row(modifier = Modifier.background(MaterialTheme.colors.onSurface)) { }
            }
        }
    }
}