package backend.html.providers

import kotlinx.html.*
import kotlinx.html.stream.createHTML

private val supportedLanguagesForRunning = listOf(
    "c", "c++", "python", "scala", "java"
)

fun makeCodeField(
    lang: String,
    blockId: String,
    fileName: String?,
    codeFound: String
) = createHTML().div(classes = "code-container") {

    val supportedRunLang = lang.lowercase() in supportedLanguagesForRunning

    p(classes = "code-container-language") {
        +lang.replaceFirstChar { it.uppercaseChar() }
        +(fileName?.let { "($it)" } ?: "")
    }
    if (supportedRunLang) {
        button(classes = "run-button") {
            id = "run-$blockId"
            +"Run Code"
        }
    }
    button(classes = "reset-button") {
        id = "reset-$blockId"
        +"Reset"
    }
    div(classes = "ace code-block") {
        id = blockId
        +codeFound
    }
    if (supportedRunLang) {
        div(classes = "output-container") {
            p(classes = "run-result-text") { id = "$blockId-run-result" }
            textArea(classes = "code-output-text") {
                id = "$blockId-output"
                readonly = true
                +"Run the code to see it's output."
            }
        }
    }
}