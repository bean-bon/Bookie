package backend

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator

class Renderer(
    private val source: String,
    private val ast: ASTNode,
    private val flavour: MarkdownFlavourDescriptor
) {

    fun defaultRender() = HtmlGenerator(
        markdownText = source,
        root = ast,
        flavour = flavour
    )

}