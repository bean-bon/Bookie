package views.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import backend.EventManager

class TextEditorViewModel(
    val openFiles: List<TextEditorEntryFieldModel>,
    initialFile: TextEditorEntryFieldModel?,
) {

    companion object {
        val permittedEditorFormats = listOf(
            "application/rtf",
            "text/markdown",
            "text/plain",
            "text/html",
            "text/css"
        )
    }

    var selectedFile by mutableStateOf(initialFile)
        private set
    var closingFile by mutableStateOf(false)

    fun updateSelected(model: TextEditorEntryFieldModel) =
        EventManager.fileSelected.publishEvent(model)

}