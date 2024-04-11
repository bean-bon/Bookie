package backend.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import backend.EventManager
import org.koin.core.component.KoinComponent
import java.nio.file.Path

object ApplicationData {

    private var titleBase by mutableStateOf("Bookie Editor")
    private var flavourText by mutableStateOf<String?>(null)
    var projectDirectory by mutableStateOf<Path?>(null)

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