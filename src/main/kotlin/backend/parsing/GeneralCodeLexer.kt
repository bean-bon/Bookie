package backend.parsing

import views.helpers.GeneralCodeSyntaxDefinition

// Regular expression for general code.
//private val codeRegex: Rexp = TODO()

val keywordsOnly: (String, String) -> Token? = { s1, s2 -> when (s1) {
    TokenKeys.KEYWORD -> T_KEYWORD(s2)
    else -> null
} }

val otherOnly: (String, String) -> Token? = { s1, s2 ->
    println("s2: $s2")
    when (s1) {
    "key" -> T_KEYWORD(s2)
    "other" -> T_OTHER(s2)
    else -> null
} }

data class GeneralLexer(
    private val reg: Rexp,
    private val tokenDefinition: (String, String) -> Token? = GeneralCodeSyntaxDefinition.tokenDef
): Tokeniser {
    override fun tokenise(s: String): List<Token> =
        lexingSimp(reg, s).mapNotNull { (s1, s2) -> tokenDefinition(s1, s2) }
}