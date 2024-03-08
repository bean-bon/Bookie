package backend.html.providers

import backend.extensions.getPath
import backend.html.ChapterInformation
import backend.html.helpers.GenerationTracker
import backend.html.helpers.IDCreator
import backend.html.helpers.PathResolver
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator

/**
 * Enhanced paragraph provider enabling support for automatic
 * referencing and KaTeX.
 * This class will only process pure paragraphs, and inlined nodes are
 * passed onto the *inlineParagraphProvider* parameter.
 * @param deferredParagraphs A map of the paragraphs being deferred, those to be deferred are added to this object.
 * @param deferredInlineBlocks A map of Markdown elements being deferred within paragraphs, those found are added to this.
 * @param chapterMarkers An optional list of chapters found in paragraphs. This should be null when chapters cannot
 * be referenced (for creation), such as from within a chapter.
 * @param inlineParagraphProvider the provider used for impure paragraphs.
 * @author Benjamin Groom
 */
class BDParagraphProvider(
    var deferredParagraphs: MutableMap<String, String>,
    var deferredInlineBlocks: MutableMap<String, String>,
    var chapterMarkers: MutableSet<ChapterInformation>? = null,
    private val inlineParagraphProvider: GeneratingProvider,
    private val buildForFlask: Boolean,
    private val onStartProcessing: () -> Unit,
    private val onQuizSyntaxRecognised: (String) -> Unit
): GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        // Check for impure paragraphs.
        val permittedNames = listOf(
            "TEXT", "WHITE_SPACE", "EOL", "BR",
            "SHORT_REFERENCE_LINK", "FULL_REFERENCE_LINK", "INLINE_LINK",
            "(", ")", ":", "'", "!"
        )
        // Ensures that quiz blocks are only recognised if they are immediately followed by the answer list.
        onStartProcessing()
        if (!node.children.all { it.type.name in permittedNames }) {
            if (checkQuizSyntax(text, node)) return
            return inlineParagraphProvider.processNode(visitor, text, node)
        }
        // Add placeholder for deferred paragraph to be replaced after the initial processing.
        var paragraph = ""
        for (v in node.children) {
            when (v.type.name) {
                "TEXT", "WHITE_SPACE", "(", ")", "BR", "EOL", ":", "'", "!" -> {
                    paragraph += v.getTextInNode(text)
                }
                else -> {
                    val blockId = IDCreator.inlineBlock.nextId
                    deferredInlineBlocks["%%$blockId"] = v.getTextInNode(text).toString()
                    paragraph += "%%$blockId"
                }
            }
        }
        if (chapterMarkers != null) {
            // Regex matches sequence of "name (path/to/file)".
            val chapterReferences = """\b(\w+)\s+\(([^)]+)\)(?:\s+|\n)*""".toRegex().findAll(paragraph)
            for (ref in chapterReferences) {
                if (!ref.groupValues[2].endsWith(".bd")) {
                    visitor.consumeHtml("<strong><p style=\"color: red\">Only .bd files may be chapters</p></strong>")
                    continue
                }
                val path = ChapterInformation(
                    name = ref.groupValues[1],
                    path = ref.groupValues[2],
                    templateMarker = IDCreator.chapter.nextId,
                    index = IDCreator.chapter.currentIndex
                )
                val newChapter = GenerationTracker.addToList(ref.groupValues[2])
                if (newChapter) {
                    chapterMarkers!!.add(path)
                    paragraph = paragraph.replace(
                        ref.groupValues.first(),
                        if (buildForFlask) {
                            val rePath = PathResolver.getRelativeFilePath(getPath(ref.groupValues[2])!!)
                            "<br><a class=\"chapter-definition\" href=\"{{ url_for('static', filename = '$rePath'}}\">Chapter " +
                            "${IDCreator.chapter.currentIndex} - ${ref.groupValues[1]}</a><br>"
                        } else {
                            "<br><a class=\"chapter-definition\" href=\"${ref.groupValues[2].replace(".bd", ".html")}\">" +
                            "Chapter ${IDCreator.chapter.currentIndex} - ${ref.groupValues[1]}</a><br>"
                        }
                    )
                } else {
                    paragraph = paragraph.replace(
                        ref.groupValues.first(),
                        "<br><strong><p style=\"color: red\">Chapter \"${ref.groupValues[2]}\" cannot be created more than once</p></strong><br>"
                    )
                }
            }
        }
        val paragraphID = IDCreator.paragraph.nextId
        deferredParagraphs["%%$paragraphID"] = "<p>$paragraph</p>"
        visitor.consumeHtml("%%$paragraphID")
    }

    /**
     * Looks for the quiz syntax by checking for text starting with "Q:" followed by an EOL,
     * and then an unordered list with nothing after it.
     */
    private fun checkQuizSyntax(text: String, node: ASTNode): Boolean {
        val potentialQuestion = node.getTextInNode(text)
        if (potentialQuestion.startsWith("Q:")) {
            onQuizSyntaxRecognised(potentialQuestion.removePrefix("Q:").trim().toString())
            return true
        }
        return false
    }
}