package backend.parsing

import org.intellij.markdown.ast.*
import org.intellij.markdown.flavours.*
import org.intellij.markdown.parser.MarkdownParser

class BookieParser(flavour: MarkdownFlavourDescriptor) {

    private val baseParser = MarkdownParser(flavour)

    fun parseText(inputText: String): ASTNode {
        val initialTree = buildAST(inputText)
        return initialTree
    }

    private fun buildAST(input: String): ASTNode = baseParser.buildMarkdownTreeFromString(input)

}