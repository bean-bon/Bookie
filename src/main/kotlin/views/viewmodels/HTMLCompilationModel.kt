package views.viewmodels

import java.nio.file.Path

data class HTMLCompilationModel(
    val path: Path,
    val html: String
)