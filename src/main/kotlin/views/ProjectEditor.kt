package views

import backend.model.ApplicationData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import views.components.SplitView
//import views.editor.BookieMDOutput
import backend.model.DirectoryModel
import backend.model.FileStorage
import views.editor.FileTree
import views.editor.TextEditor
import backend.model.ProjectEditorModel
import views.viewmodels.TextEditorViewModel

@Composable
fun ProjectEditor(
    model: ProjectEditorModel,
    onLoad: () -> Unit,
) {

    LaunchedEffect(ApplicationData.projectDirectory) {
        onLoad()
    }

    SplitView(
        defaultProportion = 0.25f,
        minProportion = 0.1f,
        maxProportion = 0.9f,
        leftView = {
            ApplicationData.projectDirectory?.let {
                FileTree(FileStorage.makeTree(it) as DirectoryModel)
            }
        },
        rightView = {
            TextEditor(
                TextEditorViewModel(
                    initialFile = model.selectedFileModel,
                    openFiles = model.openFiles.values.toList()
                )
            )
        }
    )

}
