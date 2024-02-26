package views.helpers

import backend.extensions.getPath
import java.nio.file.Path
import kotlin.io.path.name

enum class OS {
    WINDOWS,
    MAC_OS,
    UNIX
}

object SystemUtils {
    fun getPlatform(): OS = run {
        val platformName = System.getProperty("os.name")
        when {
            platformName.contains("Windows") -> OS.WINDOWS
            platformName.contains("Mac") -> OS.MAC_OS
            else -> OS.UNIX
        }
    }
    fun openFileWithDefaultApplication(path: Path) {
        ProcessBuilder(
            when (getPlatform()) {
                OS.WINDOWS -> listOf(path.name)
                OS.MAC_OS,
                OS.UNIX -> listOf("open", path.name)
            }
        )
            .directory(path.parent.toFile())
            .start()
    }
    fun getHomeFolder(): Path? = getPath(System.getProperty("user.home"))
}