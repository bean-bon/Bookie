package backend.html.providers

import backend.html.HeadingData
import backend.html.helpers.IDCreator
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import java.util.UUID

class BDHeadingProvider(
    val level: Int,
    val headingData: MutableList<HeadingData>
): GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        val headingContent = node.getTextInNode(text).dropWhile { it == '#' }.trim()
        val title = """(.+\s*)\{?""".toRegex().matchAt(headingContent, 0)?.groups?.first()
        IDCreator.Headings.resetAllFrom(level + 1)
        IDCreator.resetFigureIfNeeded(level)
        val id = IDCreator.Headings.getBuilderFor(level)?.nextId
        return headingContent.lastIndexOf('{').let {
            if (it != -1) {
                val htmlId = """(.+\s*)}$""".toRegex().matchAt(headingContent, it)?.groups?.first()
                htmlId?.value
            } else null
        }?.let {
            headingData.add(
                HeadingData(
                    index =
                        if (level < 4) IDCreator.makeSectionStringForCurrentState()
                        else "",
                    id = it,
                    content = headingContent.toString(),
                    level = level
                )
            )
            visitor.consumeHtml(
                "<h$level id=${it.drop(1).dropLast(1)}>" +
                        "${title?.value?.replace(it, "")?.trim()}" +
                        "</h$level>\n"
            )
        } ?: run {
            // Create an auto-generated heading for the contents container.
            val generatedId = id ?: UUID.randomUUID().toString()
            val sectionIndex =
                if (level < 4) IDCreator.makeSectionStringForCurrentState()
                else ""
            val content = headingContent.toString()
            headingData.add(
                HeadingData(
                    index = sectionIndex,
                    id = generatedId,
                    content = content,
                    level = level
                )
            )
            visitor.consumeHtml("<h$level id=\"$generatedId\">$sectionIndex $content</h$level>\n")
        }
    }
}