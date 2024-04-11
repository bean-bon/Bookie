package backend.model

import backend.html.helpers.CodeBlockHTMLData
import java.nio.file.Path

data class HTMLCompilationModel(
    val inputPath: Path,
    val outputRoot: Path,
    val relativeOutputPath: Path,
    val html: String,
    val fileResources: List<Path>,
    val codeBlockMapping: List<CodeBlockHTMLData>
)