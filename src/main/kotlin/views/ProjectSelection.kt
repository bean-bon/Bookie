package views

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import views.components.BasicTextLink
import views.helpers.FileDialog
import views.helpers.ImagePaths
import views.viewmodels.ProjectSelectionModel
import java.awt.FileDialog


/**
 *
 */
@Composable
fun ProjectSelection(
    model: ProjectSelectionModel,
    onClickNewProject: () -> Unit,
    onClickOpenProject: () -> Unit,
    onNewProjectDialog: (FileDialog) -> Unit,
    onOpenProjectDialog: (FileDialog) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxHeight(0.8f)) {
            // File dialog for open and new project.
            if (model.fileDialogAction != FileDialogAction.none) {
                val mode =
                    if (model.fileDialogAction == FileDialogAction.open) FileDialog.LOAD
                    else FileDialog.SAVE
                FileDialog(
                    title = model.fileDialogTitle,
                    mode = mode,
                    onCreate = { model.fileDialog = it },
                ) {
                    if (model.fileDialog != null) {
                        if (model.fileDialogAction == FileDialogAction.create) onNewProjectDialog(model.fileDialog!!)
                        else onOpenProjectDialog(model.fileDialog!!)
                    }
                    model.fileDialogAction = FileDialogAction.none
                }
            }
            // Title
            Text(
                "Bookie",
                Modifier.padding(PaddingValues(bottom = 20.dp)),
                fontWeight = FontWeight.Bold,
                fontSize = 60.sp
            )
            // Options for projects
            BasicTextLink(
                text = "New Project",
                imageResourcePath = ImagePaths.newProject,
                imageDescription = "New File",
                height = 40.dp,
                fontSize = 35.sp,
                textPaddingValues = PaddingValues(start = 10.dp),
                onClick = onClickNewProject
            )
            BasicTextLink(
                text = "Open",
                imageResourcePath = ImagePaths.folderIcon,
                imageDescription = "Choose an existing project to open.",
                height = 40.dp,
                fontSize = 35.sp,
                textPaddingValues = PaddingValues(start = 10.dp),
                onClick = onClickOpenProject
            )
//            BasicTextLink(text = "Recent", fontSize = 35.sp) {
//
//            }
         }
    }
}

enum class FileDialogAction {
    none,
    create,
    open
}