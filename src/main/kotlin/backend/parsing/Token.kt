package backend.parsing

/**
 * Referring to token types output from Lexer.sc.
 * @author Benjamin Groom
 */
sealed interface Token

data class T_KEYWORD(val k: String): Token
data class T_OP(val op: String): Token
data class T_STRING(val str: String): Token
data class T_PAREN(val par: String): Token
data class T_WHITESPACE(val space: String): Token
data class T_ID(val id: String): Token
data class T_TYPE(val ty: String): Token
data class T_NUM(val num: String): Token
data class T_OTHER(val s: String): Token

fun Token.textValue() = when (this) {
    is T_ID -> this.id
    is T_KEYWORD -> this.k
    is T_NUM -> this.num
    is T_OP -> this.op
    is T_OTHER -> this.s
    is T_WHITESPACE -> this.space
    is T_PAREN -> this.par
    is T_STRING -> this.str
    is T_TYPE -> this.ty
}