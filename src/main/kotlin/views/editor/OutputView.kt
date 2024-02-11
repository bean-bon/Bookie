package views.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.*
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.*
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext.get
import org.koin.java.KoinJavaComponent.inject
import views.viewmodels.MDOutputViewModel
import java.io.File
import kotlin.math.max

@Composable
fun BookieMDOutput(
    model: MDOutputViewModel = koinInject()
) {
    WebView(
        model.webViewState,
        Modifier.fillMaxSize(),
    )
}