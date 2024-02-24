package views

import ApplicationData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import views.components.SplitView
//import views.editor.BookieMDOutput
import backend.model.DirectoryModel
import backend.model.FileStorage
import views.editor.FileTree
import views.editor.TextEditor
import views.viewmodels.ProjectEditorModel
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
