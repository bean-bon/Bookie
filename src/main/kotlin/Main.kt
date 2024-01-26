import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import backend.parsing.BookieParser
import backend.Renderer
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import views.BookieMDOutput
import views.Editor
import views.FileTree
import views.SplitView

@Composable
@Preview
fun App() {

    var htmlContent by remember { mutableStateOf("No HTML yet, press the button for compilation") }

    MaterialTheme {
        SplitView(
            defaultProportion = 0.7f,
            minProportion = 0.5f,
            leftView = { SplitView(
                defaultProportion = 0.3f,
                minProportion = 0.1f,
                maxProportion = 0.4f,
                leftView = { FileTree() },
                rightView = { Editor(onCompile = {
                    val parser = BookieParser(GFMFlavourDescriptor())
                    val renderer = Renderer(it, parser.parseText(it), GFMFlavourDescriptor())
                    htmlContent = renderer.defaultRender().generateHtml()
                }) }
            ) },
            rightView = { BookieMDOutput(htmlContent) }
        )
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Bookie Editor"
    ) {
        App()
    }
}
