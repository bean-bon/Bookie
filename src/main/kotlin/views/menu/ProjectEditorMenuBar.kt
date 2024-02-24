package views.menu

import ApplicationData
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import backend.EventManager
import backend.PreferencePaths
import backend.PreferenceHandler
import backend.html.helpers.PathResolver
import org.koin.compose.koinInject
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
        val selectedFile = projectEditorModel.selectedFileModel?.file
        val bookieFileSelected = selectedFile?.extension == "bd"
        val contentsPage = selectedFile == ApplicationData.projectDirectory!! / "front_matter.bd"
        Item(
            "Save File",
            enabled = selectedFile?.extension == "bd",
            shortcut = KeyShortcut(Key.S, meta = macOS, ctrl = !macOS)
        ) {
            EventManager.saveSelectedFile.publishEvent()
        }
        Item(
            "Build File",
            enabled = bookieFileSelected && !contentsPage,
            shortcut = KeyShortcut(Key.B, meta = macOS, alt = macOS, shift = !macOS, ctrl = !macOS))
        {
            EventManager.buildCurrentFile.publishEvent()
        }
        Item(
            "Open file in browser",
            enabled = bookieFileSelected && !contentsPage,
            shortcut = KeyShortcut(Key.P, meta = macOS, alt = macOS, shift = !macOS, ctrl = !macOS))
        {
            projectEditorModel.selectedFileModel?.let {
                val compiledPath = PathResolver.getCompiledOutputDirectory(it.file) / "${it.file.nameWithoutExtension}.html"
                EventManager.buildFile.publishEvent(it.file)
                SystemUtils.openFileWithDefaultApplication(compiledPath)
            }
        }
    }
    Menu("Project") {
        Item("Export locally") {
            EventManager.compileProject.publishEvent()
        }
        Item("Export for Flask") {
            EventManager.compileFlaskApp.publishEvent()
        }
        Item("Close Project") {
            PreferenceHandler.clearPreference(PreferencePaths.user.lastProjectPath)
            PreferenceHandler.clearPreference(PreferencePaths.user.lastProjectName)
            EventManager.projectDirModified.publishEvent(null)
            EventManager.titleFlavourTextModified.publishEvent("")
        }
    }
}