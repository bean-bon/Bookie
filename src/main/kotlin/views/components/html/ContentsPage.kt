package views.components.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML

fun bookieContents(
    pageTitle: String,
    chapterHtml: String
): String = createHTML().html {
    head {
        title(pageTitle)
    }
    body {
        h1 {
            +pageTitle
        }
        unsafe {
            raw(chapterHtml)
        }
    }
}