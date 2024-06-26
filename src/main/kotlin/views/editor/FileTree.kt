package views.editor

import backend.model.ApplicationData
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import backend.EventManager
import backend.extensions.childCount
import backend.extensions.getPath
import backend.helpers.readTextFromResource
import backend.html.helpers.PathResolver
import backend.model.DirectoryModel
import backend.model.FileModel
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
            title = { Text("Create new folder", fontWeight = FontWeight.Bold) },
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
            ContextMenuItem("Refresh Project Contents") {
                fileTreeModel.refreshContents()
            },
            if (!(fileTreeModel.path / "front_matter.bd").exists()) ContextMenuItem("Create front matter file") {
                EventManager.createFile.publishEvent(fileTreeModel.path / "front_matter.bd")
            } else null,
            if (!(fileTreeModel.path / "bookie.css").exists()) ContextMenuItem("Regenerate bookie.css") {
                (fileTreeModel.path / "bookie.css").writeText(readTextFromResource("bookie.css"))
                EventManager.projectFilesAdded.publishEvent(listOf(FileModel(fileTreeModel.path / "bookie.css")))
            } else null
        )
    }) {
        LazyColumn (
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
//                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .semantics { this.contentDescription = "Project file tree" }
        ) {
            items(fileTreeModel.contents
                .filter { !it.isHidden }
                .sortedWith(compareBy<FileStorage> { !it.isDirectory }.thenBy { it.name.uppercase() })) { f ->
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun directory(
    model: DirectoryModel,
    leftPadding: Dp
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
                title = { Text("Create new Bookie file", fontWeight = FontWeight.Bold) },
                intendedExtension = ".bd",
                confirmText = "Create file",
                modifyExisting = false,
                onDismissRequest = { showNewFileDialog = false }
            ) {
                EventManager.createFile.publishEvent(it)
            }
        } else if (showNewDirectoryDialog) {
            fileModificationDialog(
                model.path,
                title = { Text("Create new folder", fontWeight = FontWeight.Bold) },
                confirmText = "Create folder",
                modifyExisting = false,
                onDismissRequest = { showNewDirectoryDialog = false }
            ) {
                EventManager.createDirectory.publishEvent(it)
            }
        } else if (renameDirectory) {
            fileModificationDialog(
                model.path,
                title = { Text("Rename ${PathResolver.getRelativeFilePath(model.path)} folder", fontWeight = FontWeight.Bold) },
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
                        .background(if (externalDrag) MaterialTheme.colors.onBackground else Color.Unspecified)
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
                                // This is not very efficient, but the event publish did not work
                                // for drag and drop, so here we are.
                                model.refreshContents()
                            }

                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(ImagePaths.FOLDER_ICON),
                        modifier = Modifier.height(10.dp),
                        contentDescription = "Directory: ${PathResolver.getRelativeFilePath(model.path)}"
                    )
                    Text(
                        model.path.name,
                        modifier = Modifier.padding(PaddingValues(start = 3.dp)),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        color = MaterialTheme.colors.onSurface
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
                            directory(
                                FileStorage.makeTree(f.path) as DirectoryModel,
                                leftPadding + 10.dp
                            )
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
    leftPadding: Dp
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
            title = { Text("Rename ${PathResolver.getRelativeFilePath(fileModel.path)} file", fontWeight = FontWeight.Bold) },
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
                if (TextEditorViewModel.fileAllowedForBookieEditor(fileModel.path)) {
                    EventManager.openFile.publishEvent(fileModel.path)
                }
                else
                    SystemUtils.openFileWithDefaultApplication(fileModel.path)
            }
        ) {
            // TODO: add thumbnails for all file types
            if (true || fileModel.extension == "bd") {
                Image(
                    painter = painterResource(ImagePaths.BOOK_ICON),
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
                overflow = TextOverflow.Clip,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
fun fileModificationDialog(
    file: Path,
    intendedExtension: String = "",
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

    val hasError = remember(conflictingNewFileName, startsWithNumber, reservedName) {
        conflictingNewFileName || startsWithNumber || reservedName
    }

    val errorDescription = remember(hasError) {
        if (conflictingNewFileName) "${if (file.isDirectory()) "Folder" else "File"} already exists."
        else if (startsWithNumber) "The new name cannot start with a number."
        else if (reservedName) "That name is reserved, please choose another."
        else ""
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        modifier = Modifier
            .zIndex(-1f)
            .focusRequester(focusRequester)
            .focusable(true),
        onDismissRequest = onDismissRequest,
        title = title,
        text = {
            Column {
                TextField(
                    newFileName,
                    modifier = Modifier.semantics {
                        this.text = AnnotatedString(newFileName)
                        this.contentDescription = "Enter new file name for ${PathResolver.getRelativeFilePath(file)}"
                    },
                    singleLine = true,
                    label = { Text("New file name") },
                    onValueChange = {
                        newFileName = it.replace(".", "")
                        conflictingNewFileName =
                            if (newFileName.isEmpty()) false
                            else if (modifyExisting) (file.parent / "$newFileName$intendedExtension").exists()
                            else (file / "$newFileName$intendedExtension").exists()
                        startsWithNumber = !file.isDirectory() && newFileName.firstOrNull()?.isDigit() == true
                        reservedName = newFileName in reservedNames
                    }
                )
                Text(
                    errorDescription,
                    modifier = Modifier
                        .semantics { this.text = AnnotatedString("Error for new name $newFileName. $errorDescription") }
                        .focusable(hasError),
                    color = Color(255, 0, 0, if (hasError) 255 else 0)
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

    val deletionDescription = if (file.isDirectory()) {
        "Are you sure you want to delete \"${PathResolver.getRelativeFilePath(file)}\" " +
                    "and everything inside (${file.childCount()} files)?"
    } else {
        "Are you sure you want to delete \"${PathResolver.getRelativeFilePath(file)}\"?"
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        modifier = Modifier
            .zIndex(-1f)
            .focusRequester(focusRequester)
            .focusable(true),
        onDismissRequest = onDismissRequest,
        title = {
            Column(Modifier.padding(bottom = 10.dp)) {
                Text(deletionDescription)
            }
        },
        dismissButton = {
            Button(
                onDismissRequest,
                Modifier.semantics(true) {
                    this.text = AnnotatedString("Cancel deletion of ${PathResolver.getRelativeFilePath(file)}")
                }
            ) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                {
                    EventManager.deleteFile.publishEvent(file)
                    EventManager.projectFilesDeleted.publishEvent(listOf(file))
                    onDismissRequest()
                },
                Modifier.semantics(true) {
                    this.text = AnnotatedString("Confirm deletion of ${PathResolver.getRelativeFilePath(file)}")
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