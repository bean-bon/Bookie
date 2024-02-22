package backend.html

/**
 * Translation object for Ace Editor modes, since not
 * all of them match up to Markdown exactly.
 * @author Benjamin Groom
 */
object AceLanguageTranslation {
    fun translate(lang: String) = when (lang) {
        "c++",
        "c" -> "c_cpp"
        else -> lang
    }
}