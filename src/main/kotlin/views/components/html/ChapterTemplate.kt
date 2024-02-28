package views.components.html

import backend.html.ChapterLinkInformation
import backend.html.helpers.CodeBlockHTMLData
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import backend.extensions.getPath
import backend.html.HeadingData
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

fun chapterTemplate(
    compiledHtml: String,
    codeBlocks: List<CodeBlockHTMLData> = listOf(),
    chapterLinkInformation: ChapterLinkInformation,
    contents: List<HeadingData>,
    maxContentsSectionLevel: Int = 3,
    buildFlaskTemplate: Boolean = false
) = createHTML().html {
    val nestedSize = chapterLinkInformation.currentInfo.path.split("/").size
    val rootDirectory = "../".repeat(nestedSize - 1)
    val isPreview = chapterLinkInformation.currentInfo.index == -1
    head {
        this.title =
            if (!isPreview) "Chapter ${chapterLinkInformation.currentInfo.index} - ${chapterLinkInformation.currentInfo.name}"
            else "Preview of ${getPath(chapterLinkInformation.currentInfo.path)?.nameWithoutExtension ?: "chapter"}"
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        link(
            rel = "stylesheet",
            href =
                if (buildFlaskTemplate) "{{ url_for('static', filename='bookie.css') }}"
                else "${rootDirectory}bookie.css",
            type = LinkType.textCss
        )
        if (codeBlocks.isNotEmpty()) {
            if (buildFlaskTemplate) {
                link(rel = "stylesheet", href = "{{ url_for('static', filename='ace_editor/css/ace.css') }}", type = LinkType.textCss)
                script(src = "{{ url_for('static', filename='ace_editor/src/ace.js') }}", type = ScriptType.textJavaScript) {}
            } else {
                link(rel = "stylesheet", href = "${rootDirectory}ace_editor/css/ace.css", type = LinkType.textCss)
                script(src = "${rootDirectory}ace_editor/src/ace.js", type = ScriptType.textJavaScript) {}
            }
        }

    }
    body {
        h1(classes = "chapter-title") {
            +(
                if (!isPreview)
                    "Chapter ${chapterLinkInformation.currentInfo.index} - ${chapterLinkInformation.currentInfo.name}"
                else
                    "Preview of ${chapterLinkInformation.currentInfo.name}"
            )
        }
        if (contents.isNotEmpty()) {
            div {
                id = "contents-container"
//            button(classes = "open") {
//                id = "contents-toggle"
//                onClick = "toggleContentsVisibility(this, 'contents-list')"
                p(classes = "contents-title") {
                    +"Contents"
//                    span(classes = "triangle")
                }
//            }
                div {
                    id = "contents-list"
                    for (h in contents.filter { it.level <= maxContentsSectionLevel }) {
                        div(classes = "h${h.level}-contents-link") {
                            a(href = "#${h.id}") {
                                +"${h.index} ${h.content}"
                            }
                        }
                    }
                }
            }
        }
        div("bd-blocks") {

            unsafe {
                raw(compiledHtml)
            }
        }
        script(src =
            if (buildFlaskTemplate) "{{ url_for('static', filename = 'ace_editor/src/ext-language_tools.js') }}"
            else "${rootDirectory}ace_editor/src/ext-language_tools.js") {}
        unsafe {
            raw(contentScriptingTemplate(codeBlocks, buildFlaskTemplate, chapterLinkInformation))
        }
        div("navigation-container") {
            val currentPath = getPath(chapterLinkInformation.currentInfo.path.removeSuffix(".bd") + ".html")!!
            chapterLinkInformation.previousInfo?.let {
                pre("previous-chapter-link") {
                    id = "previous-chapter"
                    val previousPath = getPath(it.path.removeSuffix(".bd") + ".html")!!
                    a(href = previousPath.relativeTo(currentPath.parent).toString()) {
                        +"Previous - ${it.name}"
                    }
                }
            }
            chapterLinkInformation.nextInfo?.let {
                pre("next-chapter-link") {
                    id = "next-chapter"
                    val nextPath = getPath(it.path.removeSuffix(".bd") + ".html")!!
                    a(href = nextPath.relativeTo(currentPath.parent).toString()) {
                        +"Next - ${it.name}"
                    }
                }
            }
            if (!isPreview) {
                pre("contents-page-link") {
                    id = "contents-page"
                    val contentsString =
                        if (buildFlaskTemplate) "/"
                        else "${"../".repeat(nestedSize - 1)}index.html"
                    a(href = contentsString) {
                        +"Contents Page"
                    }
                }
            }
        }
    }
}