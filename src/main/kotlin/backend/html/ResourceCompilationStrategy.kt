package backend.html

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.fileSize

/**
 * Flag for the compiler to decide what to do with
 * files referenced from within the .bd files.
 * @author Benjamin Groom
 */
interface ResourceCompilationStrategy {
    fun isAllowed(fileName: Path, outputDirectory: Path): Boolean
}

@Suppress("Unused")
object ResourceCompilationStrategies {
    val NONE = object: ResourceCompilationStrategy {
        override fun isAllowed(fileName: Path, outputDirectory: Path): Boolean = false
    }
    val IF_NOT_PRESENT = object: ResourceCompilationStrategy {
        override fun isAllowed(fileName: Path, outputDirectory: Path): Boolean =
            !(outputDirectory / fileName.fileName).exists()
    }
    fun ifSmallerThan(maxMB: Int) = object: ResourceCompilationStrategy {
        override fun isAllowed(fileName: Path, outputDirectory: Path): Boolean =
            (fileName.fileSize() / 1024) < maxMB
    }
    val ALWAYS = object: ResourceCompilationStrategy {
        override fun isAllowed(fileName: Path, outputDirectory: Path): Boolean = true
    }
}