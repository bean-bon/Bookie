package backend.extensions

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.relativeTo

fun recursiveCopy(source: Path, target: Path) {
    fun aux(file: Path) {
        if (file.isRegularFile())
            Files.copy(
                file,
                target / file.relativeTo(source),
                StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS
            )
        else {
            Files.createDirectory(target / file.relativeTo(source))
            file.listDirectoryEntries().forEach(::aux)
        }
    }
    Files.createDirectory(target)
    source.listDirectoryEntries().forEach(::aux)
}

