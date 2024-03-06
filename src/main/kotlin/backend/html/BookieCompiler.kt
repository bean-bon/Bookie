package backend.html

import backend.model.ApplicationData
import androidx.compose.runtime.snapshots.SnapshotStateMap
import backend.EventManager
import backend.helpers.decompressZipFile
import backend.html.helpers.IDCreator
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import views.components.html.bookieContents
import views.components.html.chapterTemplate
import backend.extensions.getPath
import backend.helpers.readTextFromResource
import backend.html.helpers.GenerationTracker
import backend.model.FileStorage
import views.viewmodels.HTMLCompilationModel
import views.viewmodels.TextEditorEntryFieldModel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

/**
 * The main compiler for .bd files.
 * @param openFiles Container for the open files in the editor, storing the most up-to-date
 * representation of files.
 * @param copyMode The strategy to be used when copying the resources used by Bookie files.
 * @author Benjamin Groom
 */
class BookieCompiler(
    private val openFiles: SnapshotStateMap<Path, TextEditorEntryFieldModel>,
) {

    companion object {
        /**
         * Used as part of the single file compilation process, writing the results
         * of a single compiled document to a corresponding file.
         */
        fun compileModelToFile(model: HTMLCompilationModel) {
            EventManager.projectFilesDeleted.publishEvent(listOf(model.outputRoot))
            val filePath = model.outputRoot / model.relativeOutputPath
            Files.createDirectories(filePath.parent)
            filePath.writeText(model.html)
            for (dependency in model.fileResources) {
                val depOutputPath = model.outputRoot / dependency.relativeTo(ApplicationData.projectDirectory!!)
                Files.createDirectories(depOutputPath.parent)
                Files.copy(
                    dependency,
                    depOutputPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            // Copy standard CSS.
            val standardCSSPath = ApplicationData.projectDirectory!! / "bookie.css"
            val outputPath = model.outputRoot / "bookie.css"
            if (standardCSSPath.exists()) {
                Files.copy(
                    standardCSSPath,
                    outputPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            if (model.fileResources.isNotEmpty()) EventManager.projectFilesAdded.publishEvent(
                model.fileResources.map { FileStorage.makeTree(it) }
            )

        }

        /**
         * Compiles the model to a file such that it conforms to a Flask app
         * project structure.
         */
        fun compileModelToFlask(model: HTMLCompilationModel) {
            EventManager.projectFilesDeleted.publishEvent(listOf(model.outputRoot))
            val filePath = model.outputRoot / "templates" / model.relativeOutputPath
            Files.createDirectories(filePath.parent)
            filePath.writeText(model.html)
            for (dependency in model.fileResources) {
                val depOutputPath = model.outputRoot / "static" / dependency.relativeTo(ApplicationData.projectDirectory!!)
                Files.createDirectories(depOutputPath.parent)
                Files.copy(
                    dependency,
                    depOutputPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            if (model.fileResources.isNotEmpty()) EventManager.projectFilesAdded.publishEvent(
                model.fileResources.map { FileStorage.makeTree(it) }
            )
            // Copy standard CSS.
            val standardCSSPath = ApplicationData.projectDirectory!! / "bookie.css"
            val outputPath = model.outputRoot / "static" / "bookie.css"
            if (standardCSSPath.exists()) {
                Files.createDirectories(outputPath.parent)
                Files.copy(
                    standardCSSPath,
                    outputPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    fun exportProjectToFlask(contentsPage: Path, outputPath: Path, bookTitle: String = "Book"): Path {
        val contentsCompilationData = getFrontMatterCompilationModel(contentsPage, outputPath, bookTitle, true)
        compileModelToFlask(contentsCompilationData.first)
        contentsCompilationData.second.referencedChapters?.let { ci ->
            val chapterModels: MutableMap<Path, HTMLCompilationModel> = mutableMapOf()
            IDCreator.resetCounters()
            compileChapters(ci.toList(), chapterModels, outputPath, buildForFlask = true)
            chapterModels.forEach {
                compileModelToFlask(it.value)
            }
            if (!(outputPath / "static" / "ace_editor").exists())
                decompressZipFile("ace_editor.zip", outputPath / "static")
            EventManager.projectFilesAdded.publishEvent(
                listOf(FileStorage.makeTree(outputPath)) + chapterModels.keys.map { FileStorage.makeTree(it) }
            )
            val appFile = buildFlaskIndexFile(chapterModels.values.toList())
            (outputPath / "app.py").writeText(appFile)
            val filesToCopy = listOf(
                "requirements.txt", "bookie.properties", "install.bat", "install.sh",
                "start.bat", "start.sh"
            )
            filesToCopy.forEach { copyTextResource(it, "flask/", outputPath) }
        }
        return outputPath / "index.html"
    }

    fun exportProject(contentsPage: Path, outputPath: Path, bookTitle: String = "Book"): Path {
        val contentsCompilationData = getFrontMatterCompilationModel(contentsPage, outputPath, bookTitle, false)
        compileModelToFile(contentsCompilationData.first)
        contentsCompilationData.second.referencedChapters?.let { ci ->
            val chapterModels: MutableMap<Path, HTMLCompilationModel> = mutableMapOf()
            IDCreator.resetCounters()
            compileChapters(ci.toList(), chapterModels, outputPath, buildForFlask = false)
            chapterModels.forEach {
                compileModelToFile(it.value)
            }
            if (!(outputPath / "ace_editor").exists())
                decompressZipFile("ace_editor.zip", outputPath)
            EventManager.projectFilesAdded.publishEvent(
                listOf(FileStorage.makeTree(outputPath)) + chapterModels.keys.map { FileStorage.makeTree(it) }
            )
        }
        return outputPath / "index.html"
    }

    private fun compileChapters(
        chapterInfo: List<ChapterInformation>,
        chapterModels: MutableMap<Path, HTMLCompilationModel>,
        outputRoot: Path,
        buildForFlask: Boolean
    ) = chapterInfo.let {
        for ((i, chap) in it.withIndex()) {
            IDCreator.resetCounters()
            if (chap.path.contains(":")) continue
            getPath(chap.path)?.let { p ->
                buildFile(
                    file = ApplicationData.projectDirectory!! / p,
                    outputRoot = outputRoot,
                    relativeOutputPath = p.parent / "${p.nameWithoutExtension}.html",
                    chapterLinkInformation = ChapterLinkInformation(
                        previousInfo = it.getOrNull(i-1),
                        currentInfo = it[i],
                        nextInfo = it.getOrNull(i+1)
                    ),
                    buildForFlask = buildForFlask
                )?.let { com ->
                    chapterModels[p] = com
                }
            }
        }
    }

    private fun getFrontMatterCompilationModel(
        contentsPage: Path,
        outputPath: Path,
        bookTitle: String = "Book",
        buildForFlask: Boolean
    ): Pair<HTMLCompilationModel, CompilationData> {
        val text = openFiles[contentsPage]?.textBoxContent ?: contentsPage.readText()
        val compilationData = CompilationData(
            contentsPage,
            referencedChapters = mutableSetOf()
        )
        val bdMarkdownFlavour = BDMarkdownFlavour(compilationData)
        val initialProcessing = firstRoundProcess(text, bdMarkdownFlavour)
        return Pair(makeContentsHTMLCompilationModel(
            inputPath = contentsPage,
            compiledHTML = initialProcessing,
            compilationData = compilationData,
            outputRoot = outputPath,
            relativeOutputPath = getPath("index.html")!!,
            title = bookTitle,
            buildForFlask = buildForFlask
        ), compilationData)
    }

    fun buildFile(
        file: Path,
        copyMode: ResourceCompilationStrategy = ResourceCompilationStrategies.IF_NOT_PRESENT,
        outputRoot: Path,
        relativeOutputPath: Path,
        chapterLinkInformation: ChapterLinkInformation,
        buildForFlask: Boolean = false
    ): HTMLCompilationModel? {
        if (file.extension != "bd") return null
        val text = openFiles[file]?.textBoxContent ?: file.readText()
        val compilationData = CompilationData(file)
        val bdMarkdownFlavour = BDMarkdownFlavour(compilationData, compileForFlask = buildForFlask)
        val initialProcessing = firstRoundProcess(text, bdMarkdownFlavour)
        IDCreator.resetCounters()
        GenerationTracker.reset()
        return makeChapterHTMLCompilationModel(
            file, outputRoot, relativeOutputPath, initialProcessing, compilationData,
            copyMode, chapterLinkInformation, buildFlaskTemplate = buildForFlask
        )
    }

    private fun makeContentsHTMLCompilationModel(
        inputPath: Path,
        outputRoot: Path,
        relativeOutputPath: Path,
        compiledHTML: String,
        title: String,
        compilationData: CompilationData,
        buildForFlask: Boolean
    ) = HTMLCompilationModel(
        inputPath = inputPath,
        outputRoot = outputRoot,
        relativeOutputPath = relativeOutputPath,
        html = bookieContents(title, compiledHTML, compilationData.codeBlockMap, buildForFlask),
        fileResources = compilationData.resourcesUtilised,
        codeBlockMapping = compilationData.codeBlockMap
    )

    private fun makeChapterHTMLCompilationModel(
        file: Path,
        outputRoot: Path,
        relativeOutputPath: Path,
        compiledHTML: String,
        compilationData: CompilationData,
        copyMode: ResourceCompilationStrategy,
        chapterLinkInformation: ChapterLinkInformation,
        buildFlaskTemplate: Boolean = false
    ) = HTMLCompilationModel(
        inputPath = file,
        outputRoot = outputRoot,
        relativeOutputPath = relativeOutputPath,
        html = chapterTemplate(
            compiledHtml = compiledHTML,
            codeBlocks = compilationData.codeBlockMap,
            chapterLinkInformation = chapterLinkInformation,
            buildFlaskTemplate = buildFlaskTemplate,
            contents = compilationData.headingData
        ),
        fileResources = compilationData.resourcesUtilised.filter {
            // Only allow local paths, otherwise they are rendered by the browser.
            if (it.toString().contains(':')) return@filter false
            copyMode.isAllowed(
                it.fileName,
                outputRoot / relativeOutputPath.parent //PathResolver.getCompiledOutputDirectory(it)
            )
        },
        codeBlockMapping = compilationData.codeBlockMap
    )

    private fun createOutputPath(dirPath: Path?, relativeContainer: Path?, fileName: String): Path {
        val base = dirPath ?: ApplicationData.projectDirectory!!
        val relativePath = relativeContainer?.let { (it / "$fileName.html").toString() } ?: "$fileName.html"
        return base / relativePath
    }

    private fun firstRoundProcess(
        rawText: String,
        flavour: BDMarkdownFlavour,
    ): String {
        val parser = MarkdownParser(flavour)
        val ast = parser.buildMarkdownTreeFromString(rawText)
        val firstRoundHTML = HtmlGenerator(rawText, ast, flavour).generateHtml()
        val referencesInsertedHTML = processParagraphs(
            firstRoundHTML,
            flavour = flavour,
        )
        IDCreator.resetCounters()
        return referencesInsertedHTML
    }

    private fun processParagraphs(
        html: String,
        flavour: BDMarkdownFlavour,
    ): String {
        var newHtml = html
        // Process the deferred inline blocks (such as inline links).
        for ((blockKey, toProcess) in flavour.compilationData.deferredInlineBlocks) {
            val inlineHTML = HtmlGenerator(
                toProcess,
                MarkdownParser(flavour).buildMarkdownTreeFromString(toProcess),
                flavour
            )
                .generateHtml()
                .replace("<body>", "")
                .replace("</body>", "")
            newHtml = newHtml.replace(
                blockKey,
                inlineHTML
            )
        }
        // Replace the paragraphs with their actual content, then the placeholders
        // within the paragraphs with the corresponding reference index if it exists.
        for ((marker, paragraph) in flavour.compilationData.deferredParagraphs) {
            var processed = paragraph
            for ((refKey, index) in flavour.compilationData.referenceMap) {
                processed = processed.replace("{$refKey}", "<a href=\"#$refKey\">$index</a>")
            }
            // Replace placeholder with processed content.
            newHtml = newHtml.replace(marker, processed)
        }
        // Replace the templates in figures with their actual index.
        for ((refKey, index) in flavour.compilationData.referenceMap) {
            newHtml = newHtml.replace("Figure {$refKey}", "Figure $index")
        }
        return newHtml
    }

    private fun buildFlaskIndexFile(models: List<HTMLCompilationModel>): String {
        val generatedRoutes = models.joinToString("\n\n\n") {
            val filePath = it.relativeOutputPath
            """
                @app.route('/$filePath')
                def ${filePath.nameWithoutExtension.lowercase().replace(" ", "_")}():
                    return render_template('$filePath')
            """.trimIndent()
        }
        val templateFile = readTextFromResource("flask/FlaskAppTemplate.py")
        return templateFile.replace("%%generatedRoutes", generatedRoutes)
    }

    private fun copyTextResource(name: String, resourcePrefix: String, outputRoot: Path) =
        (outputRoot / name).writeText(readTextFromResource("$resourcePrefix$name"))
}

