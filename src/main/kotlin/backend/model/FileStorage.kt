package backend.model

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import backend.EventManager
import backend.extensions.removeFirstWhere
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Interface for representing file trees for use in
 * the file tree. Using raw paths lead to synchronisation issues
 * for more complex flows, so this is preferred.
 * @author Benjamin Groom
 */
interface FileStorage {
    val path: Path
    val isDirectory
        get() = path.isDirectory()
    val isHidden
        get() = path.isHidden()
    val parent: Path?
        get() = path.parent
    val name
        get() = path.name
    val extension
        get() = path.extension
    val nameWithoutExtension
        get() = path.nameWithoutExtension
    companion object {
        fun makeTree(base: Path): FileStorage =
            if (base.isDirectory())
                DirectoryModel(base, base.listDirectoryEntries().map(::makeTree).toMutableStateList(), false)
            else
                FileModel(base)
    }
}

data class FileModel(override val path: Path): FileStorage
data class DirectoryModel(
    override var path: Path,
    var contents: SnapshotStateList<FileStorage>,
    var expanded: Boolean
): FileStorage {
    private val paths
        get() = contents.map { it.path }
    // Theoretically, doing this will recompose the whole file tree any time
    // the event is received.
    init {
        EventManager.renameFile.subscribeToEvents { p ->
            if (path == p.first) {
                path = p.first.parent / p.second
                contents.clear()
                contents.addAll(path
                    .listDirectoryEntries()
                    .map { FileStorage.makeTree(it) }
                )
            }
        }
        EventManager.projectFilesAdded.subscribeToEvents {
            if (path.isDirectory()) {
                val relevantFiles = it.filter { p -> p.parent == path }
                for (new in relevantFiles) {
                    if (new !in contents) contents.add(new)
                }
            }
        }
        EventManager.projectFilesDeleted.subscribeToEvents {
            if (path.isDirectory()) {
                val relevantFiles = it.filter { p -> p.parent == path }
                for (new in relevantFiles) {
                    if (new in paths) contents.removeFirstWhere { p -> p.path == new }
                }
            }
        }
    }

}