package views.components.html

import backend.html.AceLanguageTranslation
import backend.html.ChapterLinkInformation
import backend.html.helpers.CodeBlockHTMLData
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import backend.extensions.getPath
import backend.html.helpers.PathResolver
import java.net.URLDecoder
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

fun chapterTemplate(
    compiledHtml: String,
    codeBlocks: List<CodeBlockHTMLData> = listOf(),
    chapterLinkInformation: ChapterLinkInformation,
    buildFlaskTemplate: Boolean = false
) = createHTML().html {
    val nestedSize = chapterLinkInformation.currentInfo.path.split("/").size
    val aceRoot = "${"../".repeat(nestedSize - 1)}ace_editor"
    val isPreview = chapterLinkInformation.currentInfo.index == -1
    head {
        this.title =
            if (!isPreview) "Chapter ${chapterLinkInformation.currentInfo.index} - ${chapterLinkInformation.currentInfo.name}"
            else "Preview of ${getPath(chapterLinkInformation.currentInfo.path)?.nameWithoutExtension ?: "chapter"}"
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        if (codeBlocks.isNotEmpty()) {
            if (buildFlaskTemplate) {
                link(rel = "stylesheet", href = "{{ url_for('static', filename='ace_editor/css/ace.css') }}", type = LinkType.textCss)
                script(src = "{{ url_for('static', filename='ace_editor/src/ace.js') }}", type = ScriptType.textJavaScript) {}
            } else {
                link(rel = "stylesheet", href = "$aceRoot/css/ace.css", type = LinkType.textCss)
                script(src = "$aceRoot/src/ace.js", type = ScriptType.textJavaScript) {}
            }
        }

        // Static styling.
        style {
            unsafe {
                raw("""#bd-blocks {
                            background-color: mediumaquamarine;
                            padding: 5px;
                            width: 100vw;
                            align-content: center
                        }
                    """.trimIndent())
            }
        }

    }
    body {
        div {
            id = "contents"
            p { +"Table of contents here" }
        }
        div {
            id = "bd-blocks"
            h1 {
                +(
                    if (!isPreview)
                        "Chapter ${chapterLinkInformation.currentInfo.index} - ${chapterLinkInformation.currentInfo.name}"
                    else
                        "Preview of ${chapterLinkInformation.currentInfo.name}"
                )
            }
            unsafe {
                raw(compiledHtml)
            }
        }
        if (codeBlocks.isNotEmpty()) {
            script(src =
                if (buildFlaskTemplate) "{{ url_for('static', filename = 'ace_editor/src/ext-language_tools.js') }}"
                else "$aceRoot/src/ext-language_tools.js") {}
            script {
                unsafe {
                    raw("ace.require(\"ace/ext/language_tools\");\n")
                    raw("""
                        function replaceAceBlockContents(filePath, editor) {
                            fetch(filePath)
                                .then(response => {
                                    if (!response.ok) {
                                        return "Unable to read file, please reload the page or undo to reset this block."
                                    }
                                    return response.text();
                                })
                                .then(fileContent => {
                                    editor.setValue(fileContent);
                                    editor.gotoLine(0);
                                })
                        };
                        
                    """.trimIndent())
                    raw(
                        codeBlocks.joinToString("\n") {
                            val varName = "ace_${it.id}"
                            """
                            var $varName = ace.edit("${it.id}");
                            $varName.setTheme("ace/theme/twilight");
                            $varName.session.setMode("ace/mode/${AceLanguageTranslation.translate(it.language)}");
                            $varName.setOptions({
                                enableBasicAutocompletion: true,
                                enableSnippets: true,
                                enableLiveAutocompletion: false
                            });
                            """.trimIndent() +
                            (it.relativeFilePath?.let { p ->
                                getPath(chapterLinkInformation.currentInfo.path)?.let { vp ->
                                    val filename =
                                        if (buildFlaskTemplate) (vp.parent / p).normalize()
                                        else (vp / p).normalize()
                                    """
                                    const ${it.id}_reset = document.getElementById("reset-${it.id}");
                                    ${it.id}_reset.addEventListener('click', function () {
                                        replaceAceBlockContents('{{ url_for("static", filename="$filename") }}', $varName)
                                    });
                                    """.trimIndent()
                                }
                            } ?: run {
                                """
                                const ${it.id}_originalContents = "${it.originalCode}"
                                
                                ${it.id}_reset.addEventListener('click', function () {
                                    $varName.setValue(originalContents)
                                });
                                """.trimIndent()
                            })
                        }
                    )
                }
            }
        }
        div {
            val currentPath = getPath(chapterLinkInformation.currentInfo.path.removeSuffix(".bd") + ".html")!!
            id = "bottom-navigation"
            chapterLinkInformation.previousInfo?.let {
                pre {
                    id = "previous-chapter"
                    val previousPath = getPath(it.path.removeSuffix(".bd") + ".html")!!
                    a(href = previousPath.relativeTo(currentPath.parent).toString()) {
                        +"Previous - ${it.name}"
                    }
                }
            }
            chapterLinkInformation.nextInfo?.let {
                pre {
                    id = "next-chapter"
                    val nextPath = getPath(it.path.removeSuffix(".bd") + ".html")!!
                    a(href = nextPath.relativeTo(currentPath.parent).toString()) {
                        +"Next - ${it.name}"
                    }
                }
            }
            if (!isPreview) {
                pre {
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

private fun makeSourceLink(src: Path, compileForFlask: Boolean) =
    if (compileForFlask && !src.toString().contains(":")) "{{ url_for('static', filename='$src') }}"
    else src