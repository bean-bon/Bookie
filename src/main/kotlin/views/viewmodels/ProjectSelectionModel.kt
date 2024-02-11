package views.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.koin.core.component.KoinComponent
import views.FileDialogAction
import java.awt.FileDialog

class ProjectSelectionModel: KoinComponent {
    var fileDialog by mutableStateOf<FileDialog?>(null)
    var fileDialogTitle by mutableStateOf("Choose a file for the new project")
    var fileDialogAction by mutableStateOf(FileDialogAction.none)
}