package views.components.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import backend.EventManager
import backend.PreferencePaths
import backend.PreferencesHandler
import views.helpers.SystemUtils

@Composable
fun ProjectEditorMenuBar(
    scope: MenuBarScope,
): Unit = with (scope) {
    val macOS = SystemUtils.isMacOS()
    Menu("File") {
        Item("Save File", shortcut = KeyShortcut(Key.S, meta = macOS, ctrl = !macOS)) {
            EventManager.saveSelectedFile.publishEvent()
        }
        Item("Build File", shortcut = KeyShortcut(Key.B, meta = macOS, ctrl = !macOS)) {
            EventManager.buildFile.publishEvent()
        }
        Item("Export Project", shortcut = KeyShortcut(Key.B, meta = macOS, alt = macOS, shift = !macOS, ctrl = !macOS)) {
            EventManager.compileProject.publishEvent()
        }
        Item("Close Project", shortcut = KeyShortcut(Key.Q, alt = true)) {
            PreferencesHandler.clearPreference(PreferencePaths.user.lastProjectPath)
            PreferencesHandler.clearPreference(PreferencePaths.user.lastProjectName)
            EventManager.projectDirModified.publishEvent(null)
            EventManager.titleFlavourTextModified.publishEvent("")
        }
    }
}