package backend.html.helpers

import backend.model.ApplicationData
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Helper object for getting relative file paths needed for
 * compilation or general file operations.
 * This is not safe to use when the application does not have a project
 * loaded as the project directory is assumed to be non-null for convenience.
 * @author Benjamin Groom
 */
object PathResolver {
    fun getCompiledOutputDirectory(child: Path): Path =
        ApplicationData.projectDirectory!! / "out" /
                ApplicationData.projectDirectory!!.relativize(child.parent)

    fun getRelativeFilePath(child: Path): Path =
        ApplicationData.projectDirectory!!.relativize(child)
}