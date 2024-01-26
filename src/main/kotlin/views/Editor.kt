package views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import views.helpers.SyntaxHighlightingVisualTransformation

@Composable
fun Editor(
    onCompile: (String) -> (Unit)
) {

    var rawTextContent by remember { mutableStateOf("") }

    // Code editor area.
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Color.LightGray)
    ) {
        Box(Modifier.fillMaxHeight(0.1f).fillMaxWidth().background(Color.White)) {
            Text(
                modifier = Modifier.align(Alignment.CenterStart).padding(PaddingValues(start = 10.dp)),
                text = "file.bd",
                fontWeight = FontWeight.Bold
            )
            Button(
                modifier = Modifier.align(Alignment.CenterEnd).padding(PaddingValues(end = 10.dp)),
                onClick = {
                    onCompile(rawTextContent)
                }
            ) {
                Text("Generate HTML")
            }
        }
        Box(Modifier.fillMaxHeight(0.9f).fillMaxWidth()) {
            BasicTextField(
                modifier = Modifier.fillMaxSize().padding(PaddingValues(top = 20.dp)),
                value = rawTextContent,
                visualTransformation = SyntaxHighlightingVisualTransformation(),
                onValueChange = { new -> rawTextContent = new }
            )
        }
    }
}

