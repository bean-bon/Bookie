package backend.html

import backend.html.providers.*
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
 * @param baseFlavour the flavour of Markdown to use for non-Bookie syntax.
 * @param codeBlockList code block list found by the parsers.
 * @param referenceMap mapping of figures to occurrence index.
 * @param deferredParagraphs replacement tokens mapped to pure paragraphs for later processing
 * to accommodate the use of references.
 * @author Benjamin Groom
 */
class BDMarkdownFlavour(
    val compilationData: CompilationData,
    val baseFlavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
    val compileForFlask: Boolean = false
) : MarkdownFlavourDescriptor {

    override val markerProcessorFactory: MarkerProcessorFactory = baseFlavour.markerProcessorFactory
    override val sequentialParserManager: SequentialParserManager = baseFlavour.sequentialParserManager

    private var activeQuizQuestion: String? = null

    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> {

        val generatorMap = baseFlavour.createHtmlGeneratingProviders(linkMap, baseURI).toMutableMap()

        // Image provider.
        setElementGeneratorGivenKeyLambda(
            generatorMap,
            BDImageVideoGeneratingProvider(
                baseFile = compilationData.file,
                referenceMap = compilationData.referenceMap,
                resourcesUtilised = compilationData.resourcesUtilised,
                compileForFlask = compileForFlask,
                linkMap,
                baseURI
            ).makeXssSafe(true)
        ) { it.name == "IMAGE" }
        println(generatorMap.map { it.key }.joinToString("\n"))
        // Code fence provider.
        setElementGeneratorGivenKeyLambda(
            generatorMap,
            BDCodeBlockProvider(compilationData.codeBlockMap)
        ) { it.name == "CODE_FENCE" }
        // Code span provider.
        setElementGeneratorGivenKeyLambda(
            generatorMap,
            BDCodeSpanProvider(
                filePath = compilationData.file,
                codeFilesReferenced = compilationData.resourcesUtilised,
                codeBlockIDMap = compilationData.codeBlockMap
            )
        ) { it.name == "CODE_SPAN" }
        // Heading providers.
        for (i in 1..6) setElementGeneratorGivenKeyLambda(
            generatorMap,
            BDHeadingProvider(level = i, headingIds = mutableListOf())
        ) { it.name == "ATX_$i"}
        // Paragraph provider.
        setElementGeneratorGivenKeyLambda(
            generatorMap,
            BDParagraphProvider(
                deferredParagraphs = compilationData.deferredParagraphs,
                deferredInlineBlocks = compilationData.deferredInlineBlocks,
                inlineParagraphProvider = generatorMap[generatorMap.keys.first { it.name == "PARAGRAPH" }]!!,
                chapterMarkers = compilationData.referencedChapters,
                buildForFlask = compileForFlask,
                onStartProcessing = { activeQuizQuestion = null },
                onQuizSyntaxRecognised = { activeQuizQuestion = it }
            ),
        ) { it.name == "PARAGRAPH" }
        setElementGeneratorGivenKeyLambda(
            generatorMap,
            BDQuizProvider(
                { activeQuizQuestion },
                deferredInlineBlocks = compilationData.deferredInlineBlocks,
                generatorMap[generatorMap.keys.first { it.name == "UNORDERED_LIST" }]!!
            )
        ) { it.name == "UNORDERED_LIST" }
        return generatorMap
    }

    private inline fun setElementGeneratorGivenKeyLambda(
        map: MutableMap<IElementType, GeneratingProvider>,
        newGeneratingProvider: GeneratingProvider,
        crossinline keyMatch: (IElementType) -> Boolean
    ) =
        map.keys.firstOrNull(keyMatch)?.let {
            map[it] = newGeneratingProvider
        } ?: println("WARNING: could not find existing Markdown provider")

    override fun createInlinesLexer(): MarkdownLexer =
        baseFlavour.createInlinesLexer()

}