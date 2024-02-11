package views.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import backend.EventManager
import views.helpers.ImagePaths
import views.helpers.getPath
import views.viewmodels.TextEditorViewModel
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.spi.FileTypeDetector
import kotlin.io.path.*

@Composable
fun FileTree(
    projectPath: Path,
) {
    // File navigation tree.
    Column(
        modifier = Modifier
            .fillMaxHeight(1f)
            .fillMaxWidth()
            .background(Color.Cyan)
            .horizontalScroll(rememberScrollState())
    ) {
        for (f in projectPath.listDirectoryEntries()
            .filter { !it.isHidden() }
            .sortedBy { it.name }
        ) {
            directory(
                f,
                leftPadding = 5.dp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun directory(
    path: Path,
    leftPadding: Dp,
) {

    var isExpanded by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewDirectoryDialog by remember { mutableStateOf(false) }
    var externalDrag by remember { mutableStateOf(false) }

    Box(
        Modifier.padding(PaddingValues(start = leftPadding)),
        contentAlignment = Alignment.CenterStart
    ) {
        if (showNewFileDialog) {
            createNewFileDialog(
                path,
                directory = false,
                updateVisibility = { showNewFileDialog = it }
            )
        } else if (showNewDirectoryDialog) {
            createNewFileDialog(
                path,
                directory = true,
                updateVisibility = { showNewDirectoryDialog = it }
            )
        }
        Column {
            ContextMenuArea({
                listOf(
                    ContextMenuItem("New File") {
                        showNewFileDialog = true
                    },
                    ContextMenuItem("New Directory") {
                        showNewDirectoryDialog = true
                    },
                    ContextMenuItem(if (isExpanded) "Collapse Folder" else "Open Folder") {
                        isExpanded = !isExpanded
                    }
                )
            }) {
                Row(
                    Modifier
                        .combinedClickable(
                            role = Role.DropdownList,
                            onDoubleClick = { isExpanded = !isExpanded },
                            onClick = { }
                        )
                        .background(if (externalDrag) Color.Blue else Color.Unspecified)
                        .onExternalDrag(
                            onDrag = {
                                externalDrag = true
                            },
                            onDragExit = {
                                externalDrag = false
                            }
                        ) { dragValue ->
                            externalDrag = false
                            (dragValue.dragData as? DragData.FilesList)?.let { fList ->
                                val paths = fList.readFiles().map { it.replace("file:", "") }
                                paths.forEach { file ->
                                    val decodedName = URLDecoder.decode(file, "UTF-8")
                                    getPath(decodedName)?.let { copyPath ->
                                        Files.copy(
                                            copyPath,
                                            path / copyPath.name
                                        )
                                    }
                                }
                            }

                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(ImagePaths.folderIcon),
                        modifier = Modifier.height(10.dp),
                        contentDescription = "Directory"
                    )
                    Text(
                        path.name,
                        modifier = Modifier.padding(PaddingValues(start = 3.dp)),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
            if (isExpanded) {
                for (f in path.listDirectoryEntries()
                    .filter { !it.isHidden() }
                    .sortedBy { it.name }
                ) {
                    if (f.isDirectory()) {
                        directory(f, leftPadding + 10.dp)
                    } else {
                        Text(
                            text = f.name,
                            modifier = Modifier
                                .padding(start = leftPadding + 10.dp)
                                .clickable {
                                    if (BDFileTypeDetector().probeContentType(f) in TextEditorViewModel.permittedEditorFormats ||
                                        Files.probeContentType(f) in TextEditorViewModel.permittedEditorFormats) {
                                        EventManager.openFile.publishEvent(f)
                                    }
                                },
                            maxLines = 1,
                            overflow = TextOverflow.Clip)
                    }
                }
            }
        }
    }
}

@Composable
private fun createNewFileDialog(
    directoryPath: Path,
    directory: Boolean = false,
    updateVisibility: (Boolean) -> Unit
) {

    var newFileName by remember { mutableStateOf("") }
    var conflictingNewFileName by remember { mutableStateOf(false) }

    AlertDialog(
        title = {
            Text(
                "Create a new ${ if (directory) "directory" else "Bookie file" }",
            )
        },
        text = {
            Column(Modifier.padding(top = 100.dp)) {
                TextField(
                    newFileName,
                    modifier = Modifier.padding(bottom = 5.dp),
                    onValueChange = {
                        newFileName = it.replace(".", "")
                        conflictingNewFileName =
                            if (directory) Path.of(directoryPath.toString(), newFileName).exists()
                            else Path.of(directoryPath.toString(), "$newFileName.bd").exists()
                    }
                )
                Text(
                    "${ if (directory) "Directory" else "File" } already exists, choose a different name",
                    color = Color(255, 0, 0, if (conflictingNewFileName) 255 else 0)
                )
            }
        },
        onDismissRequest = { updateVisibility(false) },
        dismissButton = { Button({ updateVisibility(false) }) { Text("Cancel") } },
        confirmButton = {
            Button(
                {
                    updateVisibility(false)
                    if (directory) EventManager.createDirectory.publishEvent(directoryPath / newFileName)
                    else EventManager.createFile.publishEvent(directoryPath / newFileName)
                },
                enabled = !conflictingNewFileName
            ) {
                Text("Create File")
            }
        }
    )
}

class BDFileTypeDetector: FileTypeDetector() {
    override fun probeContentType(path: Path?): String =
        if (path?.extension == "bd") "text/markdown"
        else ""

}