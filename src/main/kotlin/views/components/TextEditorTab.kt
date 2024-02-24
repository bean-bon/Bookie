package views.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
            if (selected) MaterialTheme.colors.primarySurface
            else MaterialTheme.colors.secondaryVariant
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
                    modifier = Modifier.clickable { onClose(model) }.padding(5.dp)
                )
            }
            if (selected) {
                Row(modifier = Modifier.background(MaterialTheme.colors.onSurface)) { }
            }
        }
    }
}