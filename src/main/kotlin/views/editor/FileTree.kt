package views.editor

import ApplicationData
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import backend.EventManager
import backend.extensions.childCount
import backend.html.helpers.PathResolver
import views.helpers.ImagePaths
import views.helpers.SystemUtils
import backend.extensions.getPath
import views.viewmodels.TextEditorViewModel
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.spi.FileTypeDetector
import kotlin.io.path.*

data class DirectoryModel(
    val path: Path,
    var contents: SnapshotStateList<Path>
) {
    // Theoretically, doing this will recompose the whole file tree any time
    // the event is received.
    init {
        EventManager.projectFilesAdded.subscribeToEvents {
            val relevantFiles = it.filter { p -> p.parent == path }
            if (path.isDirectory()) {
                for (new in relevantFiles) {
                    if (new !in contents) contents.add(new)
                }
            } else {
                contents = mutableStateListOf(path)
            }
        }
        EventManager.projectFilesDeleted.subscribeToEvents {
            val relevantFiles = it.filter { p -> p.parent == path }
            if (path.isDirectory()) {
                for (new in relevantFiles) {
                    if (new in contents) contents.remove(new)
                }
            } else {
                contents = mutableStateListOf(path)
            }
        }
    }
}

@Composable
fun FileTree(
    fileTreeModel: DirectoryModel
) {

    var showNewFolderDialog by remember { mutableStateOf(false) }

    if (showNewFolderDialog) {
        createNewFileDialog(
            fileTreeModel.path,
            directory = true,
            reservedNames = listOf("ace_editor"),
            updateVisibility = { showNewFolderDialog = it }
        )
    }

    // File navigation tree.
    ContextMenuArea({
        listOfNotNull(
            ContextMenuItem("New Folder") {
                showNewFolderDialog = true
            },
            if (!(fileTreeModel.path / "front_matter.bd").exists()) ContextMenuItem("Create front matter file") {
                EventManager.createFile.publishEvent(fileTreeModel.path / "front_matter.bd")
            } else null
        )
    }) {
        Column(
            modifier = Modifier
                .fillMaxHeight(1f)
                .fillMaxWidth()
                .background(Color.Cyan)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        ) {
            for (f in fileTreeModel.contents
                .filter { !it.isHidden() }
                .sortedWith(compareBy<Path> { !it.isDirectory() }.thenBy { it.name.uppercase() })
            ) {
                if (f.isDirectory()) {
                    val contents = mutableStateListOf<Path>().apply {
                        addAll(f.listDirectoryEntries())
                    }
                    directory(
                        DirectoryModel(f, contents),
                        leftPadding = 5.dp,
                    )
                } else {
                    file(f, 5.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun directory(
    model: DirectoryModel,
    leftPadding: Dp,
) {

    var isExpanded by remember { mutableStateOf(false) }
    var externalDrag by remember { mutableStateOf(false) }

    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewDirectoryDialog by remember { mutableStateOf(false) }
    var fileForDeletion by remember { mutableStateOf<Path?>(null) }

    Box(
        Modifier.padding(PaddingValues(start = leftPadding)),
        contentAlignment = Alignment.CenterStart
    ) {
        if (showNewFileDialog) {
            createNewFileDialog(
                model.path,
                directory = false,
                updateVisibility = { showNewFileDialog = it }
            )
        } else if (showNewDirectoryDialog) {
            createNewFileDialog(
                model.path,
                directory = true,
                updateVisibility = { showNewDirectoryDialog = it }
            )
        }
        fileForDeletion?.let {
            deletionDialog(
                it,
                onDismissRequest = { fileForDeletion = null; println("set to null") }
            )
        }
        Column {
            ContextMenuArea({
                listOf(
                    ContextMenuItem("New File") {
                        showNewFileDialog = true
                    },
                    ContextMenuItem("New Folder") {
                        showNewDirectoryDialog = true
                    },
                    ContextMenuItem(if (isExpanded) "Collapse Folder" else "Open Folder") {
                        isExpanded = !isExpanded
                    },
                    ContextMenuItem("Delete Folder") {
                        fileForDeletion = model.path
                    }
                )
            }) {
                Row(
                    Modifier
                        .clickable { isExpanded = !isExpanded }
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
                                val rawPaths = fList.readFiles().map { it.replace("file:", "") }
                                val resolvedPaths = mutableListOf<Path>()
                                rawPaths.forEach { file ->
                                    val decodedName = URLDecoder.decode(file, "UTF-8")
                                    getPath(decodedName)?.let { copyPath ->
                                        Files.copy(
                                            copyPath,
                                            model.path / copyPath.name
                                        )
                                        resolvedPaths.add(copyPath)
                                    }
                                }
                                if (resolvedPaths.isNotEmpty()) {
                                    EventManager.projectFilesAdded.publishEvent(resolvedPaths)
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
                        model.path.name,
                        modifier = Modifier.padding(PaddingValues(start = 3.dp)),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
            if (isExpanded) {
                for (f in model.contents
                    .filter { !it.isHidden() }
                    .sortedWith(compareBy<Path> { !it.isDirectory() }.thenBy { it.name.uppercase() })
                ) {
                    if (f.isDirectory()) {
                        val contents = mutableStateListOf<Path>().apply {
                            addAll(f.listDirectoryEntries())
                        }
                        directory(DirectoryModel(f, contents), leftPadding + 10.dp)
                    } else {
                        file(f, leftPadding + 10.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun file(
    path: Path,
    leftPadding: Dp,
) {

    var showDeletionDialog by remember { mutableStateOf(false) }

    if (showDeletionDialog) {
        deletionDialog(path) {
            showDeletionDialog = false
        }
    }

    ContextMenuArea({
        listOfNotNull(
            if (path.extension == "bd" && path.parent != ApplicationData.projectDirectory) {
                ContextMenuItem("Open in browser") {
                    val compiledPath = PathResolver.getCompiledOutputDirectory(path) / "${path.nameWithoutExtension}.html"
                    EventManager.buildFile.publishEvent(path)
                    SystemUtils.openFileWithDefaultApplication(compiledPath)
                }
            } else null,
            ContextMenuItem("Delete File") {
                showDeletionDialog = true
            }
        )
    }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = leftPadding).clickable {
                if (TextEditorViewModel.fileAllowedForBookieEditor(path))
                    EventManager.openFile.publishEvent(path)
                else
                    SystemUtils.openFileWithDefaultApplication(path)
            }
        ) {
            // TODO: add thumbnails for all file types
            if (true || path.extension == "bd") {
                Image(
                    painter = painterResource(ImagePaths.bookIcon),
                    modifier = Modifier.height(15.dp),
                    alignment = Alignment.Center,
                    contentDescription = "Bookie file icon"
                )
            }
            Text(
                text = path.name,
                modifier = Modifier
                    .padding(PaddingValues(start = 3.dp)),
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun createNewFileDialog(
    directoryPath: Path,
    reservedNames: List<String> = listOf(),
    directory: Boolean = false,
    updateVisibility: (Boolean) -> Unit
) {

    var newFileName by remember { mutableStateOf("") }
    var conflictingNewFileName by remember { mutableStateOf(false) }
    var startsWithNumber by remember { mutableStateOf(false) }
    var reservedName by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.zIndex(-1f),
        title = {
            Text(
                "Create a new ${ if (directory) "folder" else "Bookie file" }",
            )
        },
        text = {
            Column(Modifier.padding(top = 100.dp)) {
                TextField(
                    newFileName,
                    modifier = Modifier.padding(bottom = 5.dp),
                    onValueChange = {
                        newFileName = it.replace(".", "")
                        startsWithNumber = !directory && newFileName.firstOrNull()?.isDigit() == true
                        conflictingNewFileName =
                            if (directory) (directoryPath / newFileName).exists()
                            else Path.of(directoryPath.toString(), "$newFileName.bd").exists()
                        reservedName = newFileName in reservedNames
                    }
                )
                Text(
                    if (conflictingNewFileName)
                        "${ if (directory) "Folder" else "File" } already exists, choose a different name."
                    else if (reservedName)
                        "That name is reserved, please choose a different one."
                    else if (startsWithNumber)
                        "Chapter file names are not allowed to begin with a number."
                    else "",
                    color = Color(255, 0, 0, if (conflictingNewFileName || reservedName || startsWithNumber) 255 else 0)
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
                enabled = !(newFileName.isEmpty() || conflictingNewFileName || reservedName || startsWithNumber)
            ) {
                Text("Create ${ if (directory) "folder" else "file" }")
            }
        }
    )
}

@Composable
fun deletionDialog(
    file: Path,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.zIndex(-1f),
        onDismissRequest = onDismissRequest,
        title = {
            if (file.isDirectory()) {
                Text("Are you sure you want to delete \"${PathResolver.getRelativeFilePath(file)}\" " +
                        "and everything inside (${file.childCount()} files)?")
            } else {
                Text("Are you sure you want to delete \"${PathResolver.getRelativeFilePath(file)}\"?")
            }
        },
        dismissButton = {
            Button(onDismissRequest) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                {
                    EventManager.deleteFile.publishEvent(file)
                    EventManager.projectFilesDeleted.publishEvent(listOf(file))
                    onDismissRequest()
                }
            ) {
                Text("Confirm deletion", color = Color.Red)
            }
        }
    )
}

class BDFileTypeDetector: FileTypeDetector() {
    override fun probeContentType(path: Path?): String =
        if (path?.extension == "bd") "text/markdown"
        else ""

}