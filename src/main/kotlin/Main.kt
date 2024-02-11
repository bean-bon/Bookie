import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.*
import backend.*
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import views.*
import views.components.menu.ProjectEditorMenuBar
import views.components.menu.ProjectSelectionMenuBar
import views.viewmodels.MDOutputViewModel
import views.helpers.getPath
import views.viewmodels.ProjectEditorModel
import views.viewmodels.ProjectSelectionModel
import java.awt.FileDialog
import java.io.File
import java.nio.file.Path
import kotlin.math.max

class LauncherViewModel: KoinComponent {

    val projectSelectionModel: ProjectSelectionModel by inject()
    val projectEditorModel: ProjectEditorModel by inject()

    init {
        PreferencesHandler.subscribeToPreferenceChange(
            PreferencePaths.user.lastProjectPath
        ) { ApplicationData.projectDirectory = getPath(it) }
        ApplicationData.projectDirectory = getPath(PreferencesHandler.readUserPreference(PreferencePaths.user.lastProjectPath))
        EventManager.projectDirModified.subscribeToEvents { ApplicationData.projectDirectory = it }
    }

    /** Top level declarations to allow for keyboard shortcuts. **/

    val onNewProject = {
        projectSelectionModel.fileDialogTitle = "Choose a file for the new project."
        projectSelectionModel.fileDialogAction = FileDialogAction.create
    }

    val onOpenProject = {
        projectSelectionModel.fileDialogTitle = "Open an existing project"
        projectSelectionModel.fileDialogAction = FileDialogAction.open
    }

}

/**
 * Shows one of two screens depending on if the user had a previously open project:
 * 1. the project selection screen, or
 * 2. the project editor with the relevant folder selected as root.
 */
@Composable
@Preview
fun Launcher(
    model: LauncherViewModel
) {
    MaterialTheme {
        if (ApplicationData.projectDirectory == null) {
            ProjectSelection(
                model = model.projectSelectionModel,
                onClickNewProject = model.onNewProject,
                onClickOpenProject = model.onOpenProject,
                onNewProjectDialog = {
                    if (it.file != null) {
                        setProjectPreferences(it)
                        ProjectInitialiser.initProject(it.file, it.directory)
                    }
                },
                onOpenProjectDialog = { if (it.file != null) setProjectPreferences(it) }
            )
        } else {
            ProjectEditor(
                model.projectEditorModel,
                onLoad = { EventManager.titleFlavourTextModified.publishEvent(PreferencesHandler.projectName() ?: "") },
            )
        }
    }
}

private fun setProjectPreferences(fd: FileDialog) {
    PreferencesHandler.setUserPreference(PreferencePaths.user.lastProjectName, fd.file)
    PreferencesHandler.setUserPreference(PreferencePaths.user.lastProjectPath, fd.directory + fd.file)
    EventManager.projectDirModified.publishEvent(Path.of(fd.directory, fd.file))
    EventManager.titleFlavourTextModified.publishEvent(fd.file)
}

object ApplicationData: KoinComponent {

    private var titleBase by mutableStateOf("Bookie Editor")
    private var flavourText by mutableStateOf<String?>(null)
    var projectDirectory by mutableStateOf<Path?>(null)
    val launcherViewModel: LauncherViewModel by inject()

    val windowTitle: String
        get() =
            if (!flavourText.isNullOrBlank()) "$titleBase - $flavourText"
            else titleBase

    init {
        EventManager.titleFlavourTextModified.subscribeToEvents {
            flavourText = it
        }
    }

}

private val appModule = module(createdAtStart = true) {
    single { LauncherViewModel() }
    single { ProjectSelectionModel() }
    single { MDOutputViewModel() }
    single { ProjectEditorModel(null) }
}

fun main() = application {

    var downloading by remember { mutableStateOf(0f) }
    var initialised by remember { mutableStateOf(false) }
    var restartRequired by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            KCEF.init(builder = {
                installDir(File("kcef-bundle"))
                progress {
                    onDownloading {
                        downloading = max(it, 0f)
                    }
                    onInitialized {
                        initialised = true
                    }
                }
                settings {
                    cachePath = File("kcef-cache").absolutePath
                }
            }, onError = {
                it?.printStackTrace()
            }, onRestartRequired = {
                restartRequired = true
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose { KCEF.disposeBlocking() }
    }

    Window(
      onCloseRequest = ::exitApplication,
      title = ApplicationData.windowTitle,
    ) {
        KoinApplication(application = {
            modules(appModule)
        }) {
            window.minimumSize = window.preferredSize
            MenuBar {
                if (ApplicationData.projectDirectory != null)
                    ProjectEditorMenuBar(this)
                else
                    ProjectSelectionMenuBar(
                        this,
                        ApplicationData.launcherViewModel.onNewProject,
                        ApplicationData.launcherViewModel.onOpenProject
                    )
            }
            Launcher(ApplicationData.launcherViewModel)
        }
    }

}