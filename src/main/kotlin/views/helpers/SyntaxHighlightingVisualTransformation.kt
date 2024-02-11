package views.helpers

import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import backend.parsing.*
import backend.parsing.rec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class KeywordSyntaxDefinition(
    val applicable: List<String>,
    val tokens: List<String>
)

object GeneralCodeSyntaxDefinition {

    /**
     * This val assumes that keywords were grouped such that they only ever appear once,
     * otherwise problems may occur with overwriting.
     */
    val keywordMap: Map<String, List<String>> = run {
        val data: List<KeywordSyntaxDefinition> = Json.decodeFromString<List<KeywordSyntaxDefinition>>(
            this::class.java.getResourceAsStream("/CodeKeywords.json")
                ?.bufferedReader()?.readText() ?: ""
        )
        data.map { definition ->
            definition.tokens.map {
                it to definition.applicable
            }
        }.flatten().toMap()
    }

    val tokenDef: (String, String) -> Token? = { s1, s2 -> when (s1) {
        TokenKeys.KEYWORD -> T_KEYWORD(s2)
        TokenKeys.OP -> T_OP(s2)
        TokenKeys.STRING -> T_STRING(s2)
        TokenKeys.PARENTHESIS -> T_PAREN(s2)
        TokenKeys.ID -> T_ID(s2)
        TokenKeys.WHITESPACE -> T_WHITESPACE(s2)
        TokenKeys.TYPE -> T_TYPE(s2)
        TokenKeys.NUMBER -> T_NUM(s2)
        TokenKeys.OTHER -> T_OTHER(s2)
        else -> null
    } }

     val upper = Rexp.RANGE(('A' .. 'Z').toSet())
     val lower = Rexp.RANGE(('a' .. 'z').toSet())

     val keywords = LexingHelpers.makeAlternativeChainRecord(TokenKeys.KEYWORD, keywordMap.keys.toList())
     val whitespace = TokenKeys.WHITESPACE rec ("\t".re or (" ".re.plus()) or "\n".re)
//     val types = LexingHelpers.makeAlternativeChainRecord(TokenKeys.TYPE, listOf("Int", "int", "double", "Double"))
    val types = TokenKeys.TYPE rec "Int".re
    val other = TokenKeys.OTHER rec (lower)

     val fullRegex = (whitespace).star()

    val generalCodeLexer = GeneralLexer(fullRegex, tokenDef)

}

class SyntaxHighlightingVisualTransformation(val language: String? = null): VisualTransformation {

    private fun getTokenStyle(t: Token): SpanStyle = when (t) {
        is T_ID -> SpanStyle()
        is T_KEYWORD -> SpanStyle(color = Color.Red)
        is T_NUM -> SpanStyle()
        is T_OP -> SpanStyle()
        is T_PAREN -> SpanStyle()
        is T_WHITESPACE -> SpanStyle()
        is T_STRING -> SpanStyle()
        is T_TYPE -> SpanStyle(color = Color.Magenta)
        is T_OTHER -> SpanStyle()
    }

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = buildAnnotatedString {
                println(matcher(text.text, GeneralCodeSyntaxDefinition.whitespace))
                val b = GeneralLexer(GeneralCodeSyntaxDefinition.fullRegex,
                    GeneralCodeSyntaxDefinition.tokenDef).tokenise(text.text)
                println(b)
                for (t in b) {
                    withStyle(style = getTokenStyle(t)) {
                        append(t.textValue())
                    }
                }
            },
            offsetMapping = OffsetMapping.Identity
        )
    }

}

@Serializable
data class SyntaxColourDefinition(
    private val colour: String,
    private val regex: String,
    private val exclude: List<String>?
) {
    fun getColour(): Color = Color("0xff$colour".toInt())
    fun getRegex(): Regex = Regex(regex)
    fun getExcludedCharacters() = exclude
}