package backend

import views.viewmodels.HTMLCompilationModel
import views.viewmodels.TextEditorEntryFieldModel
import java.nio.file.Path
import javax.swing.plaf.nimbus.State

/**
 * This object has some predefined event channels that classes
 * can globally publish or subscribe to using the methods in Event.
 * @author Benjamin Groom
 */
object EventManager {

    val createFile = StatefulEvent<Path>()
    val createDirectory = StatefulEvent<Path>()
    val openFile = StatefulEvent<Path>()
    val closeFile = StatefulEvent<Path>()
    val projectDirModified = StatefulEvent<Path?>()
    val titleFlavourTextModified = StatefulEvent<String>()
    val htmlCompiled = StatefulEvent<HTMLCompilationModel>()
    val fileSelected = StatefulEvent<TextEditorEntryFieldModel>()
    val saveFile = StatefulEvent<TextEditorEntryFieldModel>()

    val compileProject = StatelessEvent()
    val buildFile = StatelessEvent()
    val saveSelectedFile = StatelessEvent()

    class StatefulEvent<T: Any?> internal constructor() {
        private val subscribers = mutableListOf<(T) -> Unit>()
        fun subscribeToEvents(onEvent: (T) -> Unit) {
            subscribers.add(onEvent)
        }
        fun publishEvent(event: T) {
            subscribers.forEach { it(event) }
        }
    }

    class StatelessEvent internal constructor() {
        private val subscribers = mutableListOf<() -> Unit>()
        fun subscribeToEvents(onEvent: () -> Unit) {
            subscribers.add(onEvent)
        }
        fun publishEvent() {
            subscribers.forEach { it() }
        }
    }

}