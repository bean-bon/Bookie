package backend.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import backend.EventManager
import org.koin.core.component.KoinComponent

class ProjectExportDialogModel: KoinComponent {

    var showLocalExportDialog by mutableStateOf(false)
    var showFlaskExportDialog by mutableStateOf(false)

    var showTitleQueryDialog by mutableStateOf(false)
    var passQueryToFlask by mutableStateOf(true)
        private set

    var userTitle by mutableStateOf("")

    init {
        EventManager.compileProjectDialog.subscribeToEvents {
            showTitleQueryDialog = true
            passQueryToFlask = false
        }
        EventManager.compileFlaskDialog.subscribeToEvents {
            showTitleQueryDialog = true
            passQueryToFlask = true
        }
    }

}
