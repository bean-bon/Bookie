package views.menu

import ApplicationData
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import backend.EventManager
import backend.PreferencePaths
import backend.PreferencesHandler
import backend.html.helpers.PathResolver
import org.koin.compose.koinInject
import org.koin.java.KoinJavaComponent.inject
import views.helpers.OS
import views.helpers.SystemUtils
import views.viewmodels.ProjectEditorModel
import kotlin.io.path.div
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

@Composable
fun ProjectEditorMenuBar(
    scope: MenuBarScope,
): Unit = with (scope) {
    val projectEditorModel: ProjectEditorModel = koinInject()
    val macOS = SystemUtils.getPlatform() == OS.MAC_OS
    Menu("File") {
        Item("Save File", shortcut = KeyShortcut(Key.S, meta = macOS, ctrl = !macOS)) {
            EventManager.saveSelectedFile.publishEvent()
        }
        Item("Build File", shortcut = KeyShortcut(Key.B, meta = macOS, alt = macOS, shift = !macOS, ctrl = !macOS)) {
            EventManager.buildCurrentFile.publishEvent()
        }
        val bookieFile = projectEditorModel.selectedFileModel?.file?.extension == "bd"
        val contentsPage = projectEditorModel.selectedFileModel?.file == ApplicationData.projectDirectory!! / "front_matter.bd"
        Item(
            "Open file in browser",
            enabled = bookieFile && !contentsPage,
            shortcut = KeyShortcut(Key.P, meta = macOS, alt = macOS, shift = !macOS, ctrl = !macOS))
        {
            projectEditorModel.selectedFileModel?.let {
                val compiledPath = PathResolver.getCompiledOutputDirectory(it.file) / "${it.file.nameWithoutExtension}.html"
                EventManager.buildFile.publishEvent(it.file)
                SystemUtils.openFileWithDefaultApplication(compiledPath)
            }
        }
        Item("Export Project") {
            EventManager.compileProject.publishEvent()
        }
        Item("Export Project to Flask") {
            EventManager.compileFlaskApp.publishEvent()
        }
        Item("Close Project") {
            PreferencesHandler.clearPreference(PreferencePaths.user.lastProjectPath)
            PreferencesHandler.clearPreference(PreferencePaths.user.lastProjectName)
            EventManager.projectDirModified.publishEvent(null)
            EventManager.titleFlavourTextModified.publishEvent("")
        }
    }
}