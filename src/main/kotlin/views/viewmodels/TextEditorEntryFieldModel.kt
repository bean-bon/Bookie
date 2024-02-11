package views.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import backend.EventManager
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class TextEditorEntryFieldModel(
    val file: Path
) {

    var textBoxContent by mutableStateOf(if (file.exists()) file.readText() else "")
    var modified by mutableStateOf(false)

    init {
        EventManager.saveFile.subscribeToEvents {
            if (it == this) {
                modified = false
            }
        }
    }

    fun needsSave(): Boolean = modified

}