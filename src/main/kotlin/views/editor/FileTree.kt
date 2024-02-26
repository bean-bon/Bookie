package views.editor

import backend.model.ApplicationData
import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import backend.EventManager
import backend.extensions.childCount
import backend.extensions.getPath
import backend.html.helpers.PathResolver
import backend.model.DirectoryModel
import backend.model.FileStorage
import views.helpers.ImagePaths
import views.helpers.SystemUtils
import views.viewmodels.TextEditorViewModel
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.spi.FileTypeDetector
import kotlin.io.path.*

@Composable
fun FileTree(
    fileTreeModel: DirectoryModel
) {

    var showNewFolderDialog by remember { mutableStateOf(false) }

    if (showNewFolderDialog) {
        fileModificationDialog(
            fileTreeModel.path,
            title = { Text("Create new folder") },
            reservedNames = listOf("ace_editor"),
            confirmText = "Create folder",
            modifyExisting = false,
            onDismissRequest = { showNewFolderDialog = false }
        ) {
            EventManager.createDirectory.publishEvent(it)
        }
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
                .filter { !it.isHidden }
                .sortedWith(compareBy<FileStorage> { !it.isDirectory }.thenBy { it.name.uppercase() })
            ) {
                if (f.isDirectory) {
                    directory(
                        FileStorage.makeTree(f.path) as DirectoryModel,
                        leftPadding = 5.dp,
                    )
                } else {
                    file(FileStorage.makeTree(f.path), 5.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun directory(
    model: DirectoryModel,
    leftPadding: Dp,
) {

    var isExpanded by remember { mutableStateOf(false) }
    var externalDrag by remember { mutableStateOf(false) }

    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewDirectoryDialog by remember { mutableStateOf(false) }
    var renameDirectory by remember { mutableStateOf(false) }
    var fileForDeletion by remember { mutableStateOf<Path?>(null) }

    Box(
        Modifier.padding(PaddingValues(start = leftPadding)),
        contentAlignment = Alignment.CenterStart
    ) {
        if (showNewFileDialog) {
            fileModificationDialog(
                model.path,
                title = { Text("Create new Bookie file") },
                confirmText = "Create file",
                modifyExisting = false,
                onDismissRequest = { showNewFileDialog = false }
            ) {
                EventManager.createFile.publishEvent(it)
            }
        } else if (showNewDirectoryDialog) {
            fileModificationDialog(
                model.path,
                title = { Text("Create new folder") },
                confirmText = "Create folder",
                modifyExisting = false,
                onDismissRequest = { showNewDirectoryDialog = false }
            ) {
                EventManager.createDirectory.publishEvent(it)
            }
        } else if (renameDirectory) {
            fileModificationDialog(
                model.path,
                title = { Text("Rename ${model.path}") },
                confirmText = "Rename folder",
                modifyExisting = true,
                onDismissRequest = { renameDirectory = false }
            ) {
                EventManager.renameFile.publishEvent(Pair(model.path, it.name))
            }
        }
        fileForDeletion?.let {
            deletionDialog(it) {
                fileForDeletion = null
            }

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
                    ContextMenuItem("Rename Folder") {
                        renameDirectory = true
                    },
                    ContextMenuItem(if (isExpanded) "Collapse Folder" else "Open Folder") {
                        isExpanded = !isExpanded
                    },
                    ContextMenuItem("Refresh Contents") {
                        model.refreshContents()
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
                                model.refreshContents()
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
            AnimatedVisibility(isExpanded, Modifier.animateContentSize()) {
                Column {
                    for (f in model.contents
                        .filter { !it.isHidden }
                        .sortedWith(compareBy<FileStorage> { !it.isDirectory }.thenBy { it.name.uppercase() })
                    ) {
                        if (f.isDirectory) {
                            directory(FileStorage.makeTree(f.path) as DirectoryModel, leftPadding + 10.dp)
                        } else {
                            file(FileStorage.makeTree(f.path), leftPadding + 10.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun file(
    fileModel: FileStorage,
    leftPadding: Dp,
) {

    var showDeletionDialog by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }

    if (showDeletionDialog) {
        deletionDialog(fileModel.path) {
            showDeletionDialog = false
        }
    } else if (renaming) {
        fileModificationDialog(
            fileModel.path,
            title = { Text("Rename ${fileModel.path}") },
            confirmText = "Rename file",
            modifyExisting = true,
            onDismissRequest = { renaming = false }
        ) {
            EventManager.renameFile.publishEvent(Pair(fileModel.path, it.name))
        }
    }

    ContextMenuArea({
        (if (fileModel.extension == "bd" && fileModel.parent != ApplicationData.projectDirectory) listOf(
            ContextMenuItem("Open in browser") {
                val compiledPath = PathResolver.getCompiledOutputDirectory(fileModel.path) / "${fileModel.nameWithoutExtension}.html"
                EventManager.buildFile.publishEvent(fileModel.path)
                SystemUtils.openFileWithDefaultApplication(compiledPath)
            },
            ContextMenuItem("Rename File") {
                renaming = true
            }
        ) else listOf()) +
        listOf(
            ContextMenuItem("Delete File") {
                showDeletionDialog = true
            }
        )
    }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = leftPadding).clickable {
                if (TextEditorViewModel.fileAllowedForBookieEditor(fileModel.path))
                    EventManager.openFile.publishEvent(fileModel.path)
                else
                    SystemUtils.openFileWithDefaultApplication(fileModel.path)
            }
        ) {
            // TODO: add thumbnails for all file types
            if (true || fileModel.extension == "bd") {
                Image(
                    painter = painterResource(ImagePaths.bookIcon),
                    modifier = Modifier.height(15.dp),
                    alignment = Alignment.Center,
                    contentDescription = "Bookie file icon"
                )
            }
            Text(
                text = fileModel.name,
                modifier = Modifier
                    .padding(PaddingValues(start = 3.dp)),
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
fun fileModificationDialog(
    file: Path,
    reservedNames: List<String> = listOf(),
    title: @Composable () -> Unit,
    confirmText: String,
    modifyExisting: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmRequest: (Path) -> Unit
) {

    var newFileName by remember { mutableStateOf("") }
    var conflictingNewFileName by remember { mutableStateOf(false) }
    var startsWithNumber by remember { mutableStateOf(false) }
    var reservedName by remember { mutableStateOf(false) }

    val extension = if (!file.isDirectory()) ".${file.extension}" else ""

    AlertDialog(
        modifier = Modifier.zIndex(-1f),
        onDismissRequest = onDismissRequest,
        title = title,
        text = {
            Column(Modifier.padding(5.dp)) {
                TextField(
                    newFileName,
                    singleLine = true,
                    onValueChange = {
                        newFileName = it.replace(".", "")
                        conflictingNewFileName =
                            if (modifyExisting) (file.parent / "$newFileName$extension").exists()
                            else (file / "$newFileName$extension").exists()
                        startsWithNumber = !file.isDirectory() && newFileName.firstOrNull()?.isDigit() == true
                        reservedName = newFileName in reservedNames
                    }
                )
                Text(
                    if (conflictingNewFileName) "${if (file.isDirectory()) "Folder" else "File"} already exists."
                    else if (startsWithNumber) "The new name cannot start with a number."
                    else if (reservedName) "That name is reserved, please choose another."
                    else "",
                    color = Color(255, 0, 0, if (conflictingNewFileName || startsWithNumber || reservedName) 255 else 0)
                )
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
                    if (!modifyExisting) onConfirmRequest(file / "$newFileName$extension")
                    else onConfirmRequest(file.parent / "$newFileName$extension")
                    onDismissRequest()
                },
                enabled = !(newFileName.isEmpty() || conflictingNewFileName || startsWithNumber || reservedName)
            ) {
                Text(confirmText)
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