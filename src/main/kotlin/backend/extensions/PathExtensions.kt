package backend.extensions

import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Counts the number of files contained within a path.
 * For files, this will return 1, and symbolic links 0, otherwise the recursive
 * child count not including symbolic links.
 */
fun Path.childCount(): Int = when {
    this.isSymbolicLink() -> 0
    this.isRegularFile() -> 1
    this.isDirectory() && this.listDirectoryEntries().isEmpty() -> 1
    else ->
        File(this.toUri()).listFiles()?.mapNotNull {
            val path = it.toPath()
            if (!path.isSymbolicLink()) path.childCount()
            else null
        }?.reduceOrNull { s, t ->
            s + t
        } ?: 0
}

fun getPath(uri: String?) = if (uri != null) {
    Path(uri)
} else null