package backend

import views.helpers.getPath
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileAttribute
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
                val chapters = projectFolder / "Chapters"
                val images = projectFolder / "Images"
                Files.createDirectory(chapters)
                Files.createDirectory(images)
                return projectFolder
            }
            return null
        }
    }
}