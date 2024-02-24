package backend

import backend.extensions.getPath
import backend.helpers.decompressZipFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

class ProjectInitialiser private constructor() {
    companion object {
        @OptIn(ExperimentalPathApi::class)
        fun initProject(name: String, filePath: String): Path? {
            getPath(filePath)?.let {
                val projectFolder = it / name
                if (projectFolder.exists()) {
                    projectFolder.deleteRecursively()
                }
                Files.createDirectory(projectFolder)
                decompressZipFile("Project Template.zip", projectFolder)
                return projectFolder
            }
            return null
        }
    }
}