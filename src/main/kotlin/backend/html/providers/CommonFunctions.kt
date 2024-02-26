package backend.html.providers

import org.intellij.markdown.html.HtmlGenerator

private val supportedLanguagesForRunning = listOf(
    "c", "c++", "python", "scala", "java"
)

fun makeCodeField(
    visitor: HtmlGenerator.HtmlGeneratingVisitor,
    lang: String,
    fileName: String?,
    blockId: String,
    codeFound: String,
) {
    val supportedRunLang = lang.lowercase() in supportedLanguagesForRunning
    val runCodeButton =
        if (supportedRunLang) "<button class=\"run-button\" id=\"run-$blockId\">Run Code</button>\n"
        else ""
    val htmlFileDisplay = fileName?.let { "($it)" } ?: ""
    visitor.consumeHtml("""
        <div class="code-container">
            <p class="code-container-language">${lang.replaceFirstChar { it.uppercaseChar() }} $htmlFileDisplay</p>
            $runCodeButton
            <button class="reset-button" id="reset-$blockId">Reset</button>
            <div id="$blockId" class="ace code-block">$codeFound</div>
            ${ if (supportedRunLang) """
                <div class="output-container">
                    <p id="$blockId-run-result" class="run-result-text"></p>
                    <textarea id="$blockId-output" class="code-output-text" readonly>Run the code to see it's output.</textarea>
                </div>
            """.trimIndent() else "" }
        </div>
    """.trimIndent())
}