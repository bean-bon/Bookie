package views.components.html

import backend.extensions.getPath
import backend.helpers.readTextFromResource
import backend.html.AceLanguageTranslation
import backend.html.ChapterLinkInformation
import backend.html.helpers.CodeBlockHTMLData
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension

/**
 * Build the script which follows the Bookie output.
 * @see backend.html.providers.CommonFunctions
 * @author Benjamin Groom
 */
fun HTML.contentScriptingTemplate(
    codeBlocks: List<CodeBlockHTMLData>,
    buildForFlask: Boolean,
    chapterLinkInformation: ChapterLinkInformation
): SCRIPT.() -> Unit = {
    crossorigin = ScriptCrossorigin.anonymous
    if (codeBlocks.isNotEmpty()) {
        unsafe {
            raw(readTextFromResource("StaticCode.js"))
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
                                    if (buildForFlask) (vp.parent / p).normalize()
                                    else (vp / p).normalize()
                                val codeFilePath = getPath(it.relativeFilePath)
                                val name = codeFilePath?.let { cp -> "'${cp.nameWithoutExtension}'" } ?: "null"
                                """
                            
                                const ${it.id}_reset = document.getElementById("reset-${it.id}");
                                ${it.id}_reset.addEventListener('click', function () {
                                    replaceAceBlockContents('{{ url_for("static", filename="$filename") }}', $varName)
                                });
                                
                                const ${it.id}_run = document.getElementById("run-${it.id}");
                                ${it.id}_run.addEventListener('click', function () {
                                    runCode('${it.language}', $varName.getValue(), $name, '${it.id}-output', '${it.id}-run-result')
                                });
                                
                                """.trimIndent()
                            }
                        } ?: run {
                            """
                        
                            const ${it.id}_originalContents = "${it.originalCode}";
                            ${it.id}_reset.addEventListener('click', function () {
                                $varName.setValue(originalContents)
                            });
                            
                            """.trimIndent()
                        })
                }
            )
        }
    }
    unsafe {
        // Function for revealing quiz answers.
        raw("""
            function revealAnswer(element, parent) {
                const answers = parent.querySelectorAll('.answer');
                answers.forEach(function(child) {
                    child.classList.add('untouched-answer');
                });
                element.classList.remove('untouched-answer');
            };
            """.trimIndent()
        )
    }
}