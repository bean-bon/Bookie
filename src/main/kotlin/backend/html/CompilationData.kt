package backend.html

import backend.html.helpers.CodeBlockHTMLData
import java.nio.file.Path

/**
 * Data used during compilation to provide contextual awareness
 * for Bookie documents.
 * @param file The file being compiled.
 * @param codeBlockMap A list of code blocks contained within the document.
 * @param resourcesUtilised A list of file resources used within the document.
 * @param headingData Data about the headings contained within the document.
 * @param deferredQuizAnswers Mappings from quiz answer inlines to their Markdown content.
 * @param deferredInlineBlocks Mappings from template markers for inline Markdown elements to their raw string content.
 * @param referenceMap Mappings from a reference ID defined in figures to the ID of the figure element it represents.
 * @param referencedChapters An optional listing of chapters defined on a page, should be null when compiling chapters.
 * @author Benjamin Groom
 */
data class CompilationData(
    val file: Path,
    val codeBlockMap: MutableList<CodeBlockHTMLData> = mutableListOf(),
    val resourcesUtilised: MutableList<Path> = mutableListOf(),
    val headingData: MutableList<HeadingData> = mutableListOf(),
    val deferredInlineBlocks: MutableMap<String, String> = mutableMapOf(),
    val deferredQuizAnswers: MutableMap<String, String> = mutableMapOf(),
    val referenceMap: MutableMap<String, String> = mutableMapOf(),
    val referencedChapters: MutableSet<ChapterInformation>? = null
)