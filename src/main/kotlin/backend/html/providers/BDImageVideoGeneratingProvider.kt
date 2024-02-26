package backend.html.providers

import backend.html.helpers.IDCreator
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.ImageGeneratingProvider
import org.intellij.markdown.parser.LinkMap
import backend.extensions.getPath
import backend.html.helpers.PathResolver
import java.net.URI
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Alternative implementation of the image provider, wherein videos are also supported.
 * Captioned figures may be created using the syntax "name:" before alt text (i.e. ![figure: Caption]).
 * This yields a figure with the caption text "Caption". This works for images, but is also
 * supported for videos.
 */
class BDImageVideoGeneratingProvider(
    private val baseFile: Path,
    private var referenceMap: MutableMap<String, String>,
    private var resourcesUtilised: MutableList<Path>,
    private val compileForFlask: Boolean,
    lMap: LinkMap,
    uri: URI?
): ImageGeneratingProvider(lMap, uri) {

    override fun renderLink(
        visitor: HtmlGenerator.HtmlGeneratingVisitor,
        text: String,
        node: ASTNode,
        info: RenderInfo
    ) {

        val file = makeAbsoluteUrl(info.destination)
        var generated = false
        val mediaDescription = info.label.getTextInNode(text).drop(1).dropLast(1)

        val reference = """^\{?(.+)}?:""".toRegex().matchAt(mediaDescription, 0)?.groups?.get(1)?.value
        val makeFigure = reference != null
        val alt = if (makeFigure) mediaDescription.split(":")[1].trim() else mediaDescription

        /**
         * If a video file can be resolved, produce a video tag
         * instead of an image, will be made into a figure if the alt
         * text starts with "cap:"
         */
        getPath(file.toString())?.let {
            resourcesUtilised.add(baseFile.parent / URLDecoder.decode(info.destination.toString(), "UTF-8"))
            val probe = Files.probeContentType(it)
            if (probe?.startsWith("video") == true) {
                generated = true
                makeVideo(
                    visitor,
                    node,
                    id = reference,
                    src = makeAbsoluteUrl(info.destination),
                    alt = alt,
                    asFigure = makeFigure,
                )
            }
        }
        if (!generated) {
            makeImage(
                visitor,
                node,
                id = reference,
                src = makeAbsoluteUrl(info.destination),
                alt = alt,
                asFigure = makeFigure,
            )
        }
    }

    private fun makeImage(
        visitor: HtmlGenerator.HtmlGeneratingVisitor,
        node: ASTNode,
        id: String? = null,
        src: CharSequence,
        alt: CharSequence,
        asFigure: Boolean,
    ) {
        id?.let {
            referenceMap[it] = IDCreator.figure.nextId
        }
        if (asFigure) visitor.consumeHtml("<figure>\n")
        visitor.consumeTagOpen(
            node, "img",
            id?.let { "id=\"$it\"" } ?: "",
            "src=\"${makeSourceLink(src.toString())}\"",
            "alt=\"$alt\"",
            "class=\"figure img-figure\"",
            autoClose = true
        )
        if (asFigure) {
            val caption = id?.let { "Figure {$it}: $alt" } ?: alt
            visitor.consumeHtml("\n<figcaption class=\"caption img-figure-caption\">$caption</figcaption>\n")
            visitor.consumeHtml("</figure>\n")
        }
    }

    private fun makeVideo(
        visitor: HtmlGenerator.HtmlGeneratingVisitor,
        node: ASTNode,
        id: String? = null,
        src: CharSequence,
        alt: CharSequence,
        asFigure: Boolean,
    ) {
        id?.let {
            referenceMap[it] = IDCreator.figure.nextId
        }
        if (asFigure) visitor.consumeHtml("<figure>\n")
        visitor.consumeHtml("<p>")
        visitor.consumeTagOpen(node, "video",
            id?.let { "id=\"$it\"" } ?: "",
            "src=\"${makeSourceLink(src.toString())}\"",
            "alt=\"$alt\"",
            "class=\"figure video-figure\"",
            "controls",
            autoClose = true
        )
        visitor.consumeHtml("</p>")
        if (asFigure) {
            val caption = id?.let { "Figure {$it}: $alt" } ?: alt
            visitor.consumeHtml("\n<figcaption class=\"caption video-figure-caption\">$caption</figcaption>\n")
            visitor.consumeHtml("</figure>\n")
        }
    }

    private fun makeSourceLink(src: String) =
        if (compileForFlask && !src.contains(":")) {
            "{{ url_for('static', filename='${
                Path.of(
                    URLDecoder.decode(
                        PathResolver.getRelativeFilePath(baseFile.parent.resolve(URLDecoder.decode(src, "UTF-8"))).toString(),
                        "UTF-8"
                    )
                )
            }') }}"
        } else src
}