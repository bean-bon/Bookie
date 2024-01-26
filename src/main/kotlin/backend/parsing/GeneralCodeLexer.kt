package backend.parsing

private val allKeywords: Map<String, List<String>> = mapOf(
    "C++" to listOf(
        "align"
    )
)

// Individual regular expressions for code constructs.
private val KEYWORD =
    "if".re or "else".re or "def".re or "val".re or "var".re or
    "do".re or "while".re or


// Regular expression for general code.
//private val codeRegex: Rexp = TODO()

// Tokens.
private data class T_KEYWORD(val s: String): Token
private data class T_OP(val s: String): Token
private data class T_STRING(val s: String): Token
private data class T_PAREN(val s: String): Token
private data class T_ID(val s: String): Token
private data class T_TYPE(val s: String): Token
private data class T_NUM(val s: String): Token
private data class T_OTHER(val s: String): Token

private val tokenDef: (String, String) -> Token? = { s1, s2 -> when (s1) {
    "key" -> T_KEYWORD(s2)
    "op" -> T_OP(s2)
    "str" -> T_STRING(s2)
    "par" -> T_PAREN(s2)
    "id" -> T_ID(s2)
    "ty" -> T_TYPE(s2)
    "num" -> T_NUM(s2)
    "other" -> T_OTHER(s2)
    else -> null
} }

data class GeneralCodeLexer(private val reg: Rexp): Tokeniser {
    override fun tokenise(s: String): List<Token> =
        lexingSimp(reg, s).mapNotNull { (s1, s2) -> tokenDef(s1, s2) }
}