package backend.html.providers

import backend.html.helpers.CodeBlockHTMLData
import backend.html.helpers.IDCreator
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import java.nio.file.Path

/**
 * Generator for the custom code blocks, based on the definition
 * in Jetbrains Markdown, but with the implementation producing data
 * necessary for creating a CodeMirror field and code running facilities.
 * @see org.intellij.markdown.html.CodeFenceGeneratingProvider
 */
class BDCodeBlockProvider(
    private val codeBlockIDMap: MutableList<CodeBlockHTMLData>
): GeneratingProvider {

    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {

        // Open code block and establish indent size.
        // Here, 8 is used to capture all common indent sizes (i.e. 2 space, 4 space, tab and double tab).
        val indentPrefixSize = node.getTextInNode(text).commonPrefixWith(" ".repeat(8)).length
        val blockID = IDCreator.codeBlock.nextId

        // Get the children to process, excluding the block terminator.
        var blockChildren = node.children
        if (blockChildren.last().type == MarkdownTokenTypes.CODE_FENCE_END) {
            blockChildren = blockChildren.dropLast(1)
        }

        var hasContent = false
        var lang = ""

        // Process each child, where hasContent indicates if code has been
        // found in the block, as opposed to an empty one with some optional language.
        var codeFound = ""
        for (child in blockChildren) {
            if (hasContent && child.type in listOf(MarkdownTokenTypes.CODE_FENCE_CONTENT, MarkdownTokenTypes.EOL)) {
                val blockText = HtmlGenerator.trimIndents(
                    HtmlGenerator.leafText(text, child, false),
                    indentPrefixSize
                )
                codeFound += blockText
                visitor.consumeHtml(blockText)
            }
            else if (!hasContent && child.type == MarkdownTokenTypes.FENCE_LANG) {
                lang = HtmlGenerator.leafText(text, child).trim().split(' ')[0]
            }
            else if (!hasContent && child.type == MarkdownTokenTypes.EOL) {
                hasContent = true
            }
        }

        visitor.consumeHtml(makeCodeField(lang, blockID, fileName = null, codeFound))

        // Add the parsed content to the code block list for
        // later compilation.
        codeBlockIDMap.add(
            CodeBlockHTMLData(
                id = blockID,
                language = lang,
                relativeFilePath = null,
                originalCode = codeFound
            )
        )

        visitor.consumeHtml("</div>")

    }

}