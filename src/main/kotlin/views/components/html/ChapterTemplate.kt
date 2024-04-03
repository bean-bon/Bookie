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
) = "<!DOCTYPE html>\n" + createHTML().html {

    lang = "en-gb"

    val nestedSize = chapterLinkInformation.currentInfo.path.split("/").size
    val rootDirectory = "../".repeat(nestedSize - 1)
    val isPreview = chapterLinkInformation.currentInfo.index == -1

    val pageTitle = with(chapterLinkInformation.currentInfo) {
        if (!isPreview) "Chapter $index - $name"
        else "Preview of ${getPath(path)?.nameWithoutExtension ?: "chapter"}"
    }

    head(bookieHeader(pageTitle, buildFlaskTemplate, codeBlocks.isNotEmpty(), rootDirectory))

    body {
        h1(classes = "chapter-title") {
            +with(chapterLinkInformation.currentInfo) {
                if (!isPreview) "Chapter $index - $name"
                else "Preview of $name"
            }
        }
        if (contents.isNotEmpty()) {
            div {
                id = "contents-container"
                p(classes = "contents-title") {
                    +"Contents"
                }
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
        script(block = this@html.contentScriptingTemplate(codeBlocks, buildFlaskTemplate, chapterLinkInformation))
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