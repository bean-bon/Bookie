package backend.html.providers

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator

class BDHeadingProvider(
    val level: Int,
    val headingIds: MutableList<String>
): GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        val headingContent = node.getTextInNode(text).dropWhile { it == '#' }.trim()
        val title = """(.+\s*)\{?""".toRegex().matchAt(headingContent, 0)?.groups?.first()
        return headingContent.lastIndexOf('{').let {
            if (it != -1) {
                val htmlId = """(.+\s*)}$""".toRegex().matchAt(headingContent, it)?.groups?.first()
                htmlId?.value
            } else null
        }?.let {
            headingIds.add(it.drop(1).dropLast(1))
            visitor.consumeHtml(
                "<h$level id=${it.drop(1).dropLast(1)}>" +
                        "${title?.value?.replace(it, "")?.trim()}" +
                        "</h$level>\n"
            )
        } ?: run {
            visitor.consumeHtml("<h$level>$headingContent</h$level>\n")
        }
    }
}