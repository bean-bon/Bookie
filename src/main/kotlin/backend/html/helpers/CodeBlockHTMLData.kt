package backend.html.helpers

/**
 * Simple container for the language and <div> id of a
 * parsed code block. Typically, one of relativeFilePath
 * and originalCode will be null, but not both.
 */
data class CodeBlockHTMLData(
    val id: String,
    val language: String,
    val relativeFilePath: String?,
    val originalCode: String?
)