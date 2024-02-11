package views.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import backend.EventManager
import com.multiplatform.webview.web.WebContent
import com.multiplatform.webview.web.WebViewState
import org.koin.core.component.KoinComponent

class MDOutputViewModel: KoinComponent {

    private var htmlContent by mutableStateOf("")

    val hasContent: Boolean
        get() = htmlContent.isNotBlank()

    var webViewState by mutableStateOf(WebViewState(WebContent.Data(htmlContent)))

    init {
        EventManager.htmlCompiled.subscribeToEvents {
            htmlContent = it.html
            webViewState = WebViewState(WebContent.Data(htmlContent))
        }
        EventManager.closeFile.subscribeToEvents {
            htmlContent = ""
            webViewState = WebViewState(WebContent.Data(""))
        }
    }

}