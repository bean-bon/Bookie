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

    val katexAutoRender = """
        renderMathInElement(document.body, renderMathInElement(document.body, 
            {
              delimiters: [
                  {left: '$$', right: '$$', display: true},
                  {left: '$', right: '$', display: false}
              ],
              throwOnError : false
            }
        ));
    """

    head {
        this.title =
            with(chapterLinkInformation.currentInfo) {
                if (!isPreview) "Chapter $index - $name"
                else "Preview of ${getPath(path)?.nameWithoutExtension ?: "chapter"}"
            }
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        link(
            rel = "stylesheet",
            href =
                if (buildFlaskTemplate) "{{ url_for('static', filename='bookie.css') }}"
                else "${rootDirectory}bookie.css",
            type = LinkType.textCss
        )
        // KaTeX dependencies.
        script(
            src = "https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js",
            type = ScriptType.textJavaScript,
            crossorigin = ScriptCrossorigin.anonymous
        ) {
            defer = true
            integrity = "sha384-XjKyOOlGwcjNTAIQHIpgOno0Hl1YQqzUOEleOLALmuqehneUG+vnGctmUb0ZY0l8"
        }
        unsafe {
            raw("""
                <link rel="stylesheet" 
                href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css" 
                integrity="sha384-n8MVd4RsNIU0tAv4ct0nTaAbDJwPJzDEaqSD1odI+WdtXRGWt2kTvGFasHpSy3SV" 
                crossorigin="anonymous">
            """.trimIndent())
        }
        unsafe {
            raw("""
                <script defer 
                src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js" 
                integrity="sha384-+VBxd3r6XgURycqtZ117nYw44OOcIax56Z4dCRWbxyPt0Koah1uHoK0o4+/RRE05" 
                crossorigin="anonymous"
                onload="$katexAutoRender"></script>
            """.trimIndent())
        }
        if (codeBlocks.isNotEmpty()) {
            if (buildFlaskTemplate) {
                link(rel = "stylesheet", href = "{{ url_for('static', filename='ace_editor/css/ace.css') }}", type = LinkType.textCss)
                script(
                    src = "{{ url_for('static', filename='ace_editor/src/ace.js') }}",
                    type = ScriptType.textJavaScript,
                    crossorigin = ScriptCrossorigin.anonymous
                ) {}
            } else {
                link(rel = "stylesheet", href = "${rootDirectory}ace_editor/css/ace.css", type = LinkType.textCss)
                script(
                    src = "${rootDirectory}ace_editor/src/ace.js",
                    type = ScriptType.textJavaScript,
                    crossorigin = ScriptCrossorigin.anonymous
                ) {}
            }
        }

    }
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