package backend.html

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.nio.file.Path

fun main() {
    val md = """This is your contents page.
You could provide an abstract or short introduction.

Include chapters like they have been below

---
Introduction (Chapters/Introduction.bd)
Content (Chapters/Content.bd)"""
    val ast = MarkdownParser(BDMarkdownFlavour(CompilationData(Path.of("")))).buildMarkdownTreeFromString(md)
    HtmlGenerator(md, ast, BDMarkdownFlavour(CompilationData(Path.of(""), referencedChapters = mutableSetOf()))).generateHtml()

}