package backend.parsing.html

import org.intellij.markdown.MarkdownTokenTypes.Companion.CODE_FENCE_CONTENT
import org.intellij.markdown.MarkdownTokenTypes.Companion.CODE_FENCE_END
import org.intellij.markdown.MarkdownTokenTypes.Companion.EOL
import org.intellij.markdown.MarkdownTokenTypes.Companion.FENCE_LANG
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.ImageGeneratingProvider
import org.intellij.markdown.parser.LinkMap
import views.helpers.getPath
import views.viewmodels.TextEditorViewModel
import java.net.URI
import java.nio.file.Files

/**
 * Generating providers are used to convert IElementType objects
 * into HTML by consuming input through a visitor.
 * Custom implementations have their own class definition, otherwise
 * it is delegated to Jetbrains Markdown.
 * @author Benjamin Groom
 */

/**
 * Helper object for creating new HTML ids.
 */
internal object IDBuilder {
    private var current = 0
    fun makeID(base: String): String {
        current += 1
        return "$base-$current"
    }
}

/**
 * Simple container for the language and <div> id of a
 * parsed code block.
 */
data class CodeBlockHTMLData(
    val id: String,
    val language: String,
)

/**
 * Generator for the custom code blocks, based on the definition
 * in Jetbrains Markdown, but with the implementation producing data
 * necessary for creating a CodeMirror field and code running facilities.
 * @see org.intellij.markdown.html.CodeFenceGeneratingProvider
 */
class BDCodeBlockProvider(
    var codeBlockIDMap: MutableList<CodeBlockHTMLData>
): GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {

        // Open code block and establish indent size.
        // Here, 8 is used to capture all common indent sizes (i.e. 2 space, 4 space, tab and double tab).
        val indentPrefixSize = node.getTextInNode(text).commonPrefixWith(" ".repeat(8)).length
        val blockID = IDBuilder.makeID("codeblock")

        // The content for this div is replaced by CodeMirror in a later
        // compilation stage.
        visitor.consumeHtml("""<div id="$blockID" class="ace">""")

        // Get the children to process, excluding the block terminator.
        var blockChildren = node.children
        if (blockChildren.last().type == CODE_FENCE_END) {
            blockChildren = blockChildren.dropLast(1)
        }

        var hasContent = false
        var lang = ""

        // Process each child, where hasContent indicates if code has been
        // found in the block, as opposed to an empty one with some optional language.
        for (child in blockChildren) {
            if (hasContent && child.type in listOf(CODE_FENCE_CONTENT, EOL)) {
                visitor.consumeHtml(
                    HtmlGenerator.trimIndents(
                        HtmlGenerator.leafText(text, child, false),
                        indentPrefixSize
                    )
                )
            }
            else if (!hasContent && child.type == FENCE_LANG) {
                lang = HtmlGenerator.leafText(text, child).trim().split(' ')[0]
            }
            else if (!hasContent && child.type == EOL) {
                hasContent = true
            }
        }

        // Add the parsed content to the code block list for
        // later compilation.
        codeBlockIDMap.add(
            CodeBlockHTMLData(
                id = blockID,
                language = lang
            )
        )

        visitor.consumeHtml("</div>")

    }

}

/**
 * Alternative implementation of the image provider to also
 * support embedding video content using the HTML 5 <i>video</i> tag.
 */
class BDImageVideoGeneratingProvider(lMap: LinkMap, uri: URI?): ImageGeneratingProvider(lMap, uri) {

    override fun renderLink(
        visitor: HtmlGenerator.HtmlGeneratingVisitor,
        text: String,
        node: ASTNode,
        info: RenderInfo
    ) {
        val file = makeAbsoluteUrl(info.destination)
        var generated = false
        val mediaDescription = info.label.getTextInNode(text).drop(1).dropLast(1)
        // If a video file can be resolved, produce a video tag
        // instead of an image.
        getPath(file.toString())?.let {
            val probe = Files.probeContentType(it)
            if (probe?.startsWith("video") == true) {
                generated = true
                visitor.consumeHtml("<p>")
                visitor.consumeTagOpen(node, "video",
                    "src=\"${makeAbsoluteUrl(info.destination)}\"",
                    "alt=\"$mediaDescription\"",
                    info.title?.let { t -> "title=\"$t\"" },
                    "controls",
                    autoClose = true
                )
                visitor.consumeHtml("</p>")
            }
        }
        // Upgrade the standard image to a figure, where the alt text is
        // a caption.
        if (!generated) {
            visitor.consumeHtml("<fig>\n")
            visitor.consumeTagOpen(node, "img",
                "src=\"${makeAbsoluteUrl(info.destination)}\"",
                "alt=\"$mediaDescription\"",
                autoClose = true)
            visitor.consumeHtml("\n<figcaption>$mediaDescription</figcaption>\n")
            visitor.consumeHtml("</fig>")
        }
    }

}