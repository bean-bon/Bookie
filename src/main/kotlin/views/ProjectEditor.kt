package views

import ApplicationData
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.ResourceLoader
import org.koin.compose.koinInject
import views.components.SplitView
//import views.editor.BookieMDOutput
import views.editor.DirectoryModel
import views.editor.FileTree
import views.editor.TextEditor
import views.viewmodels.MDOutputViewModel
import views.viewmodels.ProjectEditorModel
import views.viewmodels.TextEditorViewModel
import kotlin.io.path.listDirectoryEntries

@Composable
fun ProjectEditor(
    model: ProjectEditorModel,
    onLoad: () -> Unit,
) {

    LaunchedEffect(ApplicationData.projectDirectory) {
        onLoad()
    }

    Scaffold {
        SplitView(
            defaultProportion = 0.7f,
            minProportion = 0.5f,
            leftView = {
                SplitView(
                    defaultProportion = 0.3f,
                    minProportion = 0.1f,
                    maxProportion = 0.4f,
                    leftView = {
                        ApplicationData.projectDirectory?.let {
                            val contents = it.listDirectoryEntries().toMutableStateList()
                            FileTree(
                                DirectoryModel(
                                    it,
                                    contents
                                )
                            )
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
            },
            rightView = {
                val outputModel: MDOutputViewModel = koinInject()
                if (outputModel.hasContent) {
                    // This had to be disabled because it kept hard crashing
                    Text("Preview for ${model.selectedFileModel?.file}")
//                     BookieMDOutput()
                } else {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("This is meant to be a preview of the file, but I broke it yesterday and was not " +
                                "able to fix in the time following")
                    }
                }
            }
        )
    }
}
