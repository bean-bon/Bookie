package views.helpers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SyntaxHighlightingVisualTransformation: VisualTransformation {

    companion object {
        val generalDefinition: Map<String, SyntaxColourDefinition> = 
            Json.decodeFromString(this::class.java.getResourceAsStream(
                "GeneralCodeSyntaxHighlightingDefinition.json"
            )?.bufferedReader()?.readText() ?: "")
    }

    override fun filter(text: AnnotatedString): TransformedText {
        for (def in generalDefinition.values) {
            
        }
        return TransformedText(
            text = AnnotatedString(""),
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