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
): String = createHTML().html {
    head {
        title(pageTitle)
    }
    body {
        h1 {
            +pageTitle
        }
        unsafe {
            raw(chapterHtml)
            raw(contentScriptingTemplate(codeBlocks, buildForFlask, ChapterLinkInformation.empty))
        }
    }
}