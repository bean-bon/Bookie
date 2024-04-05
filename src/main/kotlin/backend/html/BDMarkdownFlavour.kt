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
import java.util.logging.Logger

/**
 * The BDMarkdownFlavour takes a base MarkdownFlavourDescriptor for the
 * lexing, then custom HTML GeneratingProvider classes replace those from the
 * base flavour.
 * @param baseFlavour the flavour of Markdown to use for non-Bookie syntax.
 * @param compilationData Stores all information about the compilation, will be updated when generating HTML.
 * @author Benjamin Groom
 */
class BDMarkdownFlavour(
    val baseFlavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
    val compilationData: CompilationData,
    val compileForFlask: Boolean = false
) : MarkdownFlavourDescriptor {

    override val markerProcessorFactory: MarkerProcessorFactory = baseFlavour.markerProcessorFactory
    override val sequentialParserManager: SequentialParserManager = baseFlavour.sequentialParserManager

    private var activeQuizQuestion: String? = null

    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> {

        val generatorMap = baseFlavour.createHtmlGeneratingProviders(linkMap, baseURI).toMutableMap()

        // Image provider.
        setElementGeneratorGivenKey(
            generatorMap,
            BDImageVideoGeneratingProvider(
                baseFile = compilationData.file,
                referenceMap = compilationData.referenceMap,
                resourcesUtilised = compilationData.resourcesUtilised,
                compileForFlask = compileForFlask,
                linkMap,
                baseURI
            ).makeXssSafe(true),
            "IMAGE"
        )
        // Code fence provider.
        setElementGeneratorGivenKey(
            generatorMap,
            BDCodeBlockProvider(compilationData.codeBlockMap),
            "CODE_FENCE"
        )
        // Code span provider.
        setElementGeneratorGivenKey(
            generatorMap,
            BDCodeSpanProvider(
                filePath = compilationData.file,
                codeFilesReferenced = compilationData.resourcesUtilised,
                codeBlockIDMap = compilationData.codeBlockMap
            ),
            "CODE_SPAN"
        )
        // Heading providers.
        for (i in 1..6) setElementGeneratorGivenKey(
            generatorMap,
            BDHeadingProvider(level = i, headingData = compilationData.headingData),
            "ATX_$i"
        )
        setElementGeneratorGivenKey(
            generatorMap,
            BDQuizProvider(
                { activeQuizQuestion },
                deferredQuizAnswers = compilationData.deferredQuizAnswers,
                generatorMap[generatorMap.keys.first { it.name == "UNORDERED_LIST" }]!!
            ),
            "UNORDERED_LIST"
        )
        // Paragraph provider.
        setElementGeneratorGivenKey(
            generatorMap,
            BDParagraphProvider(
                deferredInlineBlocks = compilationData.deferredInlineBlocks,
                inlineParagraphProvider = generatorMap[generatorMap.keys.first { it.name == "PARAGRAPH" }]!!,
                chapterMarkers = compilationData.referencedChapters,
                buildForFlask = compileForFlask,
                onStartProcessing = { activeQuizQuestion = null },
                onQuizSyntaxRecognised = { activeQuizQuestion = it }
            ),
            "PARAGRAPH"
        )
        return generatorMap
    }

    private fun setElementGeneratorGivenKey(
        map: MutableMap<IElementType, GeneratingProvider>,
        newGeneratingProvider: GeneratingProvider,
        keyMatch: String
    ) =
        map.keys.firstOrNull { it.name == keyMatch }?.let {
            map[it] = newGeneratingProvider
        } ?: Logger.getLogger("Bookie Compiler")
            .severe("Could not find existing Markdown provider $keyMatch in the provided map.")

    override fun createInlinesLexer(): MarkdownLexer =
        baseFlavour.createInlinesLexer()

}