package views.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import backend.EventManager
import views.editor.BDFileTypeDetector
import java.nio.file.Files
import java.nio.file.Path

class TextEditorViewModel(
    val openFiles: List<TextEditorEntryFieldModel>,
    initialFile: TextEditorEntryFieldModel?,
) {

    companion object {
        private val permittedEditorFormats = listOf(
            "application/rtf",
            "text/markdown",
            "text/plain",
            "text/html",
            "text/css"
        )
        fun fileAllowedForBookieEditor(file: Path): Boolean =
            BDFileTypeDetector().probeContentType(file).isNotBlank()
            || Files.probeContentType(file)?.startsWith("text") == true
            || Files.probeContentType(file) in permittedEditorFormats
    }

    var selectedFile by mutableStateOf(initialFile)
        private set
    var closingFile by mutableStateOf(false)

    fun updateSelected(model: TextEditorEntryFieldModel) =
        EventManager.fileSelected.publishEvent(model)

}