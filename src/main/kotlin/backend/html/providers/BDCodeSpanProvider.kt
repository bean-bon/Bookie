package backend.html.providers

import backend.html.helpers.CodeBlockHTMLData
import backend.html.helpers.IDCreator
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.CodeSpanGeneratingProvider
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import backend.extensions.getPath
import backend.html.helpers.PathResolver
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Semantically identical to the standard provider, but allows for code
 * imports using the format: `c++ from ../../file.cpp`. If that syntax
 * is not recognised, a standard code span is produced.
 */
class BDCodeSpanProvider(
    private val filePath: Path,
    private val codeFilesReferenced: MutableList<Path>,
    private val codeBlockIDMap: MutableList<CodeBlockHTMLData>,
    private val defaultSpanProvider: GeneratingProvider = CodeSpanGeneratingProvider()
): GeneratingProvider {

    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        val span = node.getTextInNode(text)
        val fileImportComponents = """`(.+) from (.+|\s*)`""".toRegex().matchAt(span, 0)?.groups
        if (fileImportComponents == null) {
            // If special code span is not recognised, use the standard generator.
            defaultSpanProvider.processNode(visitor, text, node)
            return
        }
        getPath(fileImportComponents[2]?.value)?.let {
            val relativePath = getPath(it.toString().replace("\\", "/").trim())!!
            val referencedFile = filePath.parent / relativePath
            val mimeType = Files.probeContentType(referencedFile)
            if (referencedFile.exists() && !referencedFile.isDirectory() &&
                (mimeType == null || mimeType.contains("text"))
            ) {
                val contents = referencedFile.readText()
                val language = fileImportComponents[1]!!.value
                val blockId = IDCreator.codeBlock.nextId

                visitor.consumeHtml(makeCodeField(language, blockId, referencedFile.name, contents))

                codeFilesReferenced.add(referencedFile)
                codeBlockIDMap.add(
                    CodeBlockHTMLData(
                        id = blockId,
                        language = language,
                        relativeFilePath = relativePath.toString(),
                        originalCode = null
                    )
                )
                return
            } else {
                visitor.consumeHtml("<p style=\"color: red\"><strong>Couldn't resolve \"$it\" to a valid file</strong></p>")
            }
        } ?: defaultSpanProvider.processNode(visitor, text, node)

    }

}