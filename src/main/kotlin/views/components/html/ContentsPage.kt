package views.components.html

import backend.html.ChapterLinkInformation
import backend.html.helpers.CodeBlockHTMLData
import kotlinx.html.*
import kotlinx.html.stream.createHTML

fun bookieContents(
    pageTitle: String,
    chapterHtml: String,
    codeBlocks: List<CodeBlockHTMLData>,
    buildForFlask: Boolean
): String = "<!DOCTYPE html>\n" + createHTML().html {

    head(block = bookieHeader(pageTitle, buildForFlask, codeBlocks.isNotEmpty(), ""))

    body {
        h1(classes = "chapter-title") {
            +pageTitle
        }
        div(classes = "bd-blocks") {
            unsafe {
                raw(chapterHtml)
            }
        }
        script(block = this@html.contentScriptingTemplate(codeBlocks, buildForFlask, ChapterLinkInformation.empty))
    }
}