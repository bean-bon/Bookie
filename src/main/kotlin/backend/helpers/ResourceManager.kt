package backend.helpers

import androidx.compose.ui.res.useResource
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.div

/**
 * Gets the text content from a specified resource.
 */
fun readTextFromResource(name: String): String = useResource(name) {
    it.bufferedReader().use { br ->
        br.readText()
    }
}

/**
 * Convenience function for decompressing zip files by providing a resource name.
 */
fun decompressZipFile(resourceName: String, outputRoot: Path) = useResource(resourceName) {
    decompressZipFile(it, outputRoot)
}

/**
 * Decompresses an InputStream (assumed to point at a zip file) to
 * the specified output path.
 */
fun decompressZipFile(
    stream: InputStream,
    outputRoot: Path
) = ZipInputStream(stream).use { zis ->
    var zipEntry = zis.nextEntry
    while (zipEntry != null) {
        val entryOutput = outputRoot / zipEntry.name
        val fileOutput = entryOutput.toFile()
        if (zipEntry.isDirectory) {
            fileOutput.mkdirs()
        } else {
            fileOutput.parentFile?.mkdirs()
            FileOutputStream(fileOutput).use {
                zis.copyTo(it)
            }
        }
        zis.closeEntry()
        zipEntry = zis.nextEntry
    }
}