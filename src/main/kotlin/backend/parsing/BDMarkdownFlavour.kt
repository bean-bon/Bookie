package backend.parsing

import backend.parsing.html.BDCodeBlockProvider
import backend.parsing.html.BDImageVideoGeneratingProvider
import backend.parsing.html.CodeBlockHTMLData
import org.intellij.markdown.IElementType
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.URI
import org.intellij.markdown.html.makeXssSafe
import org.intellij.markdown.lexer.MarkdownLexer
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkerProcessorFactory
import org.intellij.markdown.parser.sequentialparsers.SequentialParserManager

/**
 * The BDMarkdownFlavour takes a base MarkdownFlavourDescriptor for the
 * lexing, then custom HTML GeneratingProvider classes replace those from the
 * base flavour.
 * @author Benjamin Groom
 */
class BDMarkdownFlavour(
    private val baseFlavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
    var codeBlockMap: MutableList<CodeBlockHTMLData> = mutableListOf()
) : MarkdownFlavourDescriptor {

    override val markerProcessorFactory: MarkerProcessorFactory = baseFlavour.markerProcessorFactory
    override val sequentialParserManager: SequentialParserManager = baseFlavour.sequentialParserManager

    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> {
        val generatorMap = baseFlavour.createHtmlGeneratingProviders(linkMap, baseURI).toMutableMap()
        setElementGeneratorGivenKeyLambda(
            generatorMap,
            BDImageVideoGeneratingProvider(linkMap, baseURI).makeXssSafe(true)
        ) { it.name == "IMAGE" }
        println(generatorMap.map { it.key }.joinToString("\n"))
        setElementGeneratorGivenKeyLambda(
            generatorMap,
            BDCodeBlockProvider(codeBlockMap)
        ) { it.name == "CODE_FENCE" }
        return generatorMap
    }

    private inline fun setElementGeneratorGivenKeyLambda(
        map: MutableMap<IElementType, GeneratingProvider>,
        newGeneratingProvider: GeneratingProvider,
        crossinline keyMatch: (IElementType) -> Boolean
    ) =
        map.keys.firstOrNull(keyMatch)?.let {
            map[it] = newGeneratingProvider
        }

    override fun createInlinesLexer(): MarkdownLexer =
        baseFlavour.createInlinesLexer()

}