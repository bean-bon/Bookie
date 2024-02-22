package views.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame

@Composable
fun FileDialog(
    title: String = "Choose a file",
    frame: Frame? = null,
    mode: Int = FileDialog.LOAD,
    onCreate: (FileDialog) -> Unit = {},
    onClose: (String?) -> Unit = {},
) = AwtWindow(
    create = {
        val fd = object : FileDialog(frame, title, mode) {
            override fun setVisible(b: Boolean) {
                super.setVisible(b)
                if (b) {
                    onClose(file)
                }
            }
        }
        onCreate(fd)
        fd
    },
    dispose = FileDialog::dispose
)

