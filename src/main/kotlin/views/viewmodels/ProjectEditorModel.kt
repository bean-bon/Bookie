package views.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import backend.EventManager
import backend.parsing.BDMarkdownFlavour
import backend.parsing.html.CodeBlockHTMLData
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
//import backend.parsing.flavour.parseExtendedSyntax
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

class ProjectEditorModel(
    selected: TextEditorEntryFieldModel?
) {

    init {
        EventManager.createFile.subscribeToEvents(::createNewFile)
        EventManager.createDirectory.subscribeToEvents(::createNewDirectory)
        EventManager.openFile.subscribeToEvents(::setSelectedFile)
        EventManager.saveFile.subscribeToEvents(::saveFile)
        EventManager.saveSelectedFile.subscribeToEvents(::saveSelectedFile)
        EventManager.closeFile.subscribeToEvents(::closeFile)
        EventManager.buildFile.subscribeToEvents(::buildCurrentFile)
        EventManager.htmlCompiled.subscribeToEvents(::compileModelToFile)
        EventManager.fileSelected.subscribeToEvents { selectedFileModel = it; buildCurrentFile() }
    }

    var selectedFileModel: TextEditorEntryFieldModel? by mutableStateOf(selected)
        private set

    // Text editor fields.

    val openFiles = mutableStateMapOf<Path, TextEditorEntryFieldModel>()

    private fun compileProject(): Nothing = TODO()

//    private fun printTree(node: ASTNode, display: Boolean = true): Unit = when (node) {
//        is LeafASTNode ->
//            if (display) println("leaf: ${node.type}")
//            else print("")
//        is CompositeASTNode -> {
//            println("composite: ${node.type}")
//            node.children.forEach { printTree(it, node.type.name == "Markdown:IMAGE") }
//        }
//        else -> println("iother")
//    }

    private fun buildCurrentFile() {
        selectedFileModel?.let {
            if (it.file.extension != "bd") return
            val bdMarkdownFlavour = BDMarkdownFlavour()
            val codeBlockMap = mutableListOf<CodeBlockHTMLData>()
            val parser = MarkdownParser(bdMarkdownFlavour)
            val ast = parser.buildMarkdownTreeFromString(it.textBoxContent)
//            printTree(ast)
            val compilationModel = HTMLCompilationModel(
                it.file,
                HtmlGenerator(it.textBoxContent, ast, bdMarkdownFlavour).generateHtml()
            )
            EventManager.htmlCompiled.publishEvent(compilationModel)
        }
    }

    private fun compileModelToFile(model: HTMLCompilationModel) {
        ApplicationData.projectDirectory?.let { ppath ->
            val baseFilePath = model.path.relativeTo(ppath)
            val outputPath =
                Path.of(ppath.toString(), "out") /
                baseFilePath.parent /
                "${baseFilePath.nameWithoutExtension}.html"
            Files.createDirectories(outputPath.parent)
            var baseFile = this::class.java.getResourceAsStream("/CompiledBookieTemplate.html")
                ?.bufferedReader()?.readText() ?: ""

            outputPath.writeText(model.html.split("\n").filter { !it.startsWith("%%") }.joinToString("\n"))
        }
    }

    private fun createNewFile(path: Path) {
        val newFile = path.parent / "${path.nameWithoutExtension}.bd"
        Files.createFile(newFile)
        setSelectedFile(path)
    }

    private fun createNewDirectory(path: Path) {
        Files.createDirectory(path)
    }

    private fun saveSelectedFile() {
        selectedFileModel?.let {
            EventManager.saveFile.publishEvent(it)
        }
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
        buildCurrentFile()
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