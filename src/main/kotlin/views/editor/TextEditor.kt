package views.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import backend.EventManager
import views.components.TextEditorTab
import views.viewmodels.TextEditorViewModel
import kotlin.io.path.name


@Composable
fun TextEditor(
    model: TextEditorViewModel
) {

    // Code editor area.
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Color.White)
    ) {
        if (model.closingFile) {
            AlertDialog(
                title = { Text("${model.selectedFile?.file?.name} has unsaved changes, would you like to save them?") },
                onDismissRequest = { model.closingFile = false },
                buttons = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Cancel close with no changes
                        Button(
                            { model.closingFile = false },
                            modifier = Modifier.padding(end = 10.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray)
                        ) {
                            Text("Cancel")
                        }
                        // Close without saving
                        Button(
                            {
                                model.closingFile = false
                                model.selectedFile?.let { EventManager.closeFile.publishEvent(it.file) }
                            },
                            modifier = Modifier.padding(end = 10.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                        ) {
                            Text("Close without saving")
                        }
                        // Save file
                        Button({
                            model.closingFile = false
                            model.selectedFile?.let {
                                EventManager.saveFile.publishEvent(it)
                                EventManager.closeFile.publishEvent(it.file)
                            }
                        }) {
                            Text("Save")
                        }
                    }
                }
            )
        }
        Box(Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
        ) {
            var scrollOffset by remember { mutableStateOf(0f) }
            Row(
                Modifier
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = ScrollableState {
                            scrollOffset += it
                            it
                        }
                    )
                    .horizontalScroll(ScrollState(scrollOffset.toInt()))
                    .semantics { this.contentDescription = "Open file tab list" }
            ) {
                for (file in model.openFiles) {
                    TextEditorTab(
                        file,
                        selected = file == model.selectedFile,
                        onSelect = { model.updateSelected(it) },
                        onClose = {
                            EventManager.fileSelected.publishEvent(it)
                            if (model.selectedFile?.needsSave() == true) model.closingFile = true
                            else model.selectedFile?.let { it1 -> EventManager.closeFile.publishEvent(it1.file) }
                        }
                    )
                }
            }
        }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            model.selectedFile?.let {
                TextEditorEntryField(it)
            } ?: run {
                Text(
                    "Select a file to edit from the file tree",
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    }
}

