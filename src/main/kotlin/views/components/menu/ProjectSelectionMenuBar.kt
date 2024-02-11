package views.components.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import views.helpers.SystemUtils

@Composable
fun ProjectSelectionMenuBar(
    scope: MenuBarScope,
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit,
) = with (scope) {
    val macOS: Boolean = SystemUtils.isMacOS()
    Menu("File") {
        Item("New Book", shortcut = KeyShortcut(Key.N, meta = macOS, ctrl = !macOS)) {
            onNewProject()
        }
        Item("Open Book", shortcut = KeyShortcut(Key.O, meta = macOS, ctrl = !macOS)) {
            onOpenProject()
        }
    }
}