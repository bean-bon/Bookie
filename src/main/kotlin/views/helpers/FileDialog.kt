package views.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.AwtWindow
import backend.extensions.getPath
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import kotlin.io.path.div

@Composable
fun FileDialog(
    title: String = "Choose a file",
    frame: Frame? = null,
    mode: Int = FileDialog.LOAD,
    onCreate: (FileDialog) -> Unit = {},
    onClose: (Path?) -> Unit = {},
) = AwtWindow(
    create = {
        val fd = object : FileDialog(frame, title, mode) {
            override fun setVisible(b: Boolean) {
                super.setVisible(b)
                if (b) {
                    onClose(getPath(directory)?.let { it / file })
                }
            }
        }
        onCreate(fd)
        fd
    },
    dispose = FileDialog::dispose
)

