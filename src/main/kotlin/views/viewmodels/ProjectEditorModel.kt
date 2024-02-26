package views.viewmodels

import backend.model.ApplicationData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import backend.EventManager
import backend.helpers.decompressZipFile
import backend.html.BookieCompiler
import backend.html.ChapterInformation
import backend.html.ChapterLinkInformation
import backend.html.helpers.PathResolver
import org.koin.core.component.KoinComponent
import backend.model.FileModel
import backend.model.FileStorage
//import backend.parsing.flavour.parseExtendedSyntax
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

class ProjectEditorModel(
    selected: TextEditorEntryFieldModel?
): KoinComponent {

    init {
        EventManager.createFile.subscribeToEvents(::createNewFile)
        EventManager.createDirectory.subscribeToEvents(::createNewDirectory)
        EventManager.openFile.subscribeToEvents(::setSelectedFile)
        EventManager.renameFile.subscribeToEvents(::renameFile)
        EventManager.saveFile.subscribeToEvents(::saveFile)
        EventManager.saveSelectedFile.subscribeToEvents(::saveSelectedFile)
        EventManager.closeFile.subscribeToEvents(::closeFile)
        EventManager.deleteFile.subscribeToEvents(::deleteFile)
        EventManager.buildCurrentFile.subscribeToEvents { selectedFileModel?.let { buildFile(it.file) } }
        EventManager.buildFile.subscribeToEvents(::buildFile)
        EventManager.htmlCompiled.subscribeToEvents { BookieCompiler.compileModelToFile(it) }
        EventManager.fileSelected.subscribeToEvents { selectedFileModel = it; buildFile(it.file) }
        EventManager.compileProject.subscribeToEvents(::compileProject)
        EventManager.compileFlaskApp.subscribeToEvents(::compileFlaskApp)
    }

    var selectedFileModel: TextEditorEntryFieldModel? by mutableStateOf(selected)
        private set

    // Text editor fields.

    val openFiles = mutableStateMapOf<Path, TextEditorEntryFieldModel>()

    private fun compileProject(outputPath: Path): Boolean {
        val contentsPage = ApplicationData.projectDirectory!! / "front_matter.bd"
        return if (contentsPage.exists()) {
            BookieCompiler(openFiles).exportProject(
                contentsPage,
                outputPath,
                bookTitle = outputPath.name
            )
            true
        } else false
    }

    private fun compileFlaskApp(outputPath: Path): Boolean {
        val contentsPage = ApplicationData.projectDirectory!! / "front_matter.bd"
        return if (contentsPage.exists()) {
            BookieCompiler(openFiles).exportProjectToFlask(
                contentsPage,
                outputPath,
                bookTitle = outputPath.name
            )
            true
        } else false
    }

    /**
     * This method is intended for building a single file to the
     * corresponding HTML output. For the whole project, *compileProject()* should
     * be favoured.
     */
    private fun buildFile(path: Path) {
        val outFolder = ApplicationData.projectDirectory!! / "out"
        val relativePath = PathResolver.getRelativeFilePath(path)
        if (!outFolder.exists()) {
            Files.createDirectories(outFolder)
            EventManager.projectFilesAdded.publishEvent(listOf(FileStorage.makeTree(outFolder)))
        }
        BookieCompiler(openFiles).buildFile(
            path,
            outputRoot = ApplicationData.projectDirectory!! / "out",
            relativeOutputPath = (relativePath.parent ?: relativePath) / "${path.nameWithoutExtension}.html",
            chapterLinkInformation = ChapterLinkInformation(
                null,
                ChapterInformation(relativePath.toString(), path.name, -1, ""),
                null
            )
        )?.let {
                EventManager.htmlCompiled.publishEvent(it)
            }
        if (!(ApplicationData.projectDirectory!! / "out" / "ace_editor").exists()) {
            decompressZipFile("ace_editor.zip", ApplicationData.projectDirectory!! / "out")
            EventManager.projectFilesAdded.publishEvent(listOf(FileStorage.makeTree(outFolder / "ace_editor")))
        }
        EventManager.projectFilesAdded.publishEvent(listOf(FileStorage.makeTree(ApplicationData.projectDirectory!! / "out")))
    }

    private fun renameFile(path: Pair<Path, String>) {
        val newPath = path.first.parent / path.second
        Files.move(path.first, newPath)
        EventManager.projectFilesDeleted.publishEvent(listOf(path.first))
        // If file is renamed and the tab is open, replace the tab
        if (openFiles.containsKey(path.first)) {
            val model = openFiles[path.first]!!
            openFiles[newPath] = TextEditorEntryFieldModel(newPath, model.textBoxContent, model.modified)
            closeFile(path.first)
            selectedFileModel = openFiles[newPath]
        }
        EventManager.projectFilesAdded.publishEvent(listOf(FileModel(newPath)))
        // Check if any open files are a child of the renamed directory.
        for ((k, v) in openFiles.minus(newPath)) {
            if (k.startsWith(newPath.parent)) {
                val wasSelected = k == selectedFileModel?.file
                val newEntry = newPath.parent / k.name
                openFiles[newEntry] = TextEditorEntryFieldModel(newEntry.normalize(), v.textBoxContent, v.modified)
                if (wasSelected) selectedFileModel = openFiles[newEntry]
                else closeFile(k)
                EventManager.projectFilesAdded.publishEvent(listOf(FileStorage.makeTree(newEntry)))
            }
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun deleteFile(path: Path) {
        if (path.isDirectory()) path.deleteRecursively()
        else path.deleteIfExists()
        closeFile(path)
        for (mod in openFiles) {
            if (mod.key.startsWith(path)) closeFile(mod.key)
        }
        EventManager.projectFilesDeleted.publishEvent(listOf(path))
    }

    /**
     * Creates a new Bookie file at the specified path.
     */
    private fun createNewFile(path: Path) {
        val newFile = path.parent / "${path.nameWithoutExtension}.bd"
        Files.createFile(newFile)
        setSelectedFile(newFile)
        EventManager.projectFilesAdded.publishEvent(listOf(FileStorage.makeTree(newFile)))
    }

    private fun createNewDirectory(path: Path) {
        Files.createDirectory(path)
        EventManager.projectFilesAdded.publishEvent(listOf(FileStorage.makeTree(path)))
    }

    private fun saveSelectedFile() {
        selectedFileModel?.let {
            EventManager.saveFile.publishEvent(it)
            EventManager.buildFile.publishEvent(it.file)
        }
    }

    fun saveAllFiles() = openFiles.forEach {
        EventManager.saveFile.publishEvent(it.value)
    }

    private fun saveFile(model: TextEditorEntryFieldModel) {
        model.file.writeText(model.textBoxContent)
    }

    private fun setSelectedFile(path: Path?) {
        if (path == null) {
            selectedFileModel = null
        }
        val currentRepresentation = openFiles[path]
        currentRepresentation?.let {
            selectedFileModel = it
        } ?: path?.let {
            selectedFileModel = TextEditorEntryFieldModel(it)
            selectedFileModel?.let { model ->
                openFiles[it] = model
            }
        }
        selectedFileModel?.file?.let {
            if (it == ApplicationData.projectDirectory!! / "front_matter.bd") return@let
            buildFile(it)
        }
    }

    private fun closeFile(path: Path) {
        openFiles[path]?.let {
            val modelIndex = openFiles.values.indexOf(it)
            val nextFileIndex =
                if (openFiles.size == 1) null
                else if (modelIndex == openFiles.size - 1) modelIndex - 1
                else modelIndex + 1
            val nextFile = nextFileIndex?.let { ind -> openFiles.values.toList()[ind] }
            openFiles.remove(it.file)
            setSelectedFile(nextFile?.file)
        }
    }

}