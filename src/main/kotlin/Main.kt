import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import androidx.compose.ui.zIndex
import backend.*
import org.koin.compose.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import views.*
import views.menu.ProjectEditorMenuBar
import views.menu.ProjectSelectionMenuBar
import views.viewmodels.MDOutputViewModel
import backend.extensions.getPath
import backend.model.ApplicationData
import backend.model.ProjectExportDialogModel
import org.koin.compose.koinInject
import views.helpers.FileDialog
import views.helpers.SystemUtils
import views.viewmodels.ProjectEditorModel
import views.viewmodels.ProjectSelectionModel
import java.awt.FileDialog
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name

class LauncherViewModel: KoinComponent {

    val projectSelectionModel: ProjectSelectionModel by inject()
    val projectEditorModel: ProjectEditorModel by inject()

    init {
        PreferenceHandler.subscribeToPreferenceChange(
            PreferencePaths.user.lastProjectPath
        ) { ApplicationData.projectDirectory = getPath(it) }
        ApplicationData.projectDirectory = getPath(PreferenceHandler.readUserPreference(PreferencePaths.user.lastProjectPath))
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
    model: LauncherViewModel = koinInject(),
    projectExportDialogModel: ProjectExportDialogModel = koinInject()
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
            Box {
                projectExportDialogs(projectExportDialogModel)
                ProjectEditor(
                    model.projectEditorModel,
                    onLoad = { EventManager.titleFlavourTextModified.publishEvent(PreferenceHandler.projectName() ?: "") },
                )
            }
        }
    }
}

@Composable
fun projectExportDialogs(
    model: ProjectExportDialogModel = koinInject(),
    modifier: Modifier = Modifier.zIndex(1f)
) {
    Box(modifier) {
        if (model.showLocalExportDialog) {
            FileDialog(
                title = "Export your book",
                mode = FileDialog.SAVE,
                onCreate = { SystemUtils.getHomeFolder()?.let { f ->
                    it.directory = (f / "Documents").toString()
                    it.file = "${ApplicationData.projectDirectory!!.name} (Exported)"
                } }
            ) {
                it?.let { p ->
                    EventManager.compileProject.publishEvent(p)
                    model.showLocalExportDialog = false
                }
            }
        } else if (model.showFlaskExportDialog) {
            FileDialog(
                title = "Export your book as a Flask application",
                mode = FileDialog.SAVE,
                onCreate = { SystemUtils.getHomeFolder()?.let { f ->
                    it.directory = (f / "Documents").toString()
                    it.file = "${ApplicationData.projectDirectory!!.name} (Flask)"
                } }
            ) {
                it?.let { p ->
                    EventManager.compileFlaskApp.publishEvent(p)
                    model.showFlaskExportDialog = false
                }
            }
        }
    }
}


private fun setProjectPreferences(fd: FileDialog) {
    PreferenceHandler.setUserPreference(PreferencePaths.user.lastProjectName, fd.file)
    PreferenceHandler.setUserPreference(PreferencePaths.user.lastProjectPath, fd.directory + fd.file)
    EventManager.projectDirModified.publishEvent(Path.of(fd.directory, fd.file))
    EventManager.titleFlavourTextModified.publishEvent(fd.file)
}

private val appModule = module(createdAtStart = true) {
    single { LauncherViewModel() }
    single { ProjectSelectionModel() }
    single { MDOutputViewModel() }
    single { ProjectEditorModel(null) }
    single { ProjectExportDialogModel() }
}

fun main() = application {

    Window(
      onCloseRequest = ::exitApplication,
      title = ApplicationData.windowTitle,
    ) {
        KoinApplication(application = {
            modules(appModule)
        }) {
            Box(Modifier.fillMaxSize()) {
                window.minimumSize = window.preferredSize
                MenuBar {
                    if (ApplicationData.projectDirectory != null)
                        ProjectEditorMenuBar(this)
                    else
                        ProjectSelectionMenuBar(this)
                }
                Launcher()
            }
        }
    }

}