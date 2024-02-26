package backend.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import backend.EventManager
import org.koin.core.component.KoinComponent

class ProjectExportDialogModel: KoinComponent {

    var showLocalExportDialog by mutableStateOf(false)
    var showFlaskExportDialog by mutableStateOf(false)

    init {
        EventManager.compileProjectDialog.subscribeToEvents { showLocalExportDialog = true }
        EventManager.compileFlaskDialog.subscribeToEvents { showFlaskExportDialog = true }
    }

}
