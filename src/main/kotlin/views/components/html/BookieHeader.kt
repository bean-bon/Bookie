package views.components.html

import kotlinx.html.*

fun HTML.bookieHeader(
    titleString: String,
    buildForFlask: Boolean,
    codeBlocksExist: Boolean,
    resourceRoot: String
): HEAD.() -> Unit = {

    val katexAutoRender = """
        renderMathInElement(document.body, 
            {
              delimiters: [
                  {left: '$$', right: '$$', display: true},
                  {left: '/$', right: '$/', display: false}
              ],
              throwOnError : false
            }
        );
    """

    meta(charset = "UTF-8")
    title(titleString)

    link(
        href =
            if (buildForFlask) "{{ url_for('static', filename='bookie.css') }}"
            else "${resourceRoot}bookie.css",
        rel = "stylesheet",
        type = LinkType.textCss
    )

    // KaTeX dependencies.
    script(
        src = "https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js",
        crossorigin = ScriptCrossorigin.anonymous
    ) {
        defer = true
        integrity = "sha384-XjKyOOlGwcjNTAIQHIpgOno0Hl1YQqzUOEleOLALmuqehneUG+vnGctmUb0ZY0l8"
    }
    unsafe {
        raw("""
                <link rel="stylesheet" 
                href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css" 
                integrity="sha384-n8MVd4RsNIU0tAv4ct0nTaAbDJwPJzDEaqSD1odI+WdtXRGWt2kTvGFasHpSy3SV" 
                crossorigin="anonymous">
            """.trimIndent())
    }
    unsafe {
        raw(
            """
                <script defer 
                src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js" 
                integrity="sha384-+VBxd3r6XgURycqtZ117nYw44OOcIax56Z4dCRWbxyPt0Koah1uHoK0o4+/RRE05" 
                crossorigin="anonymous"
                onload="$katexAutoRender"></script>
            """.trimIndent()
        )
    }

    if (codeBlocksExist) {
        if (buildForFlask) {
            link(rel = "stylesheet", href = "{{ url_for('static', filename='ace_editor/css/ace.css') }}", type = LinkType.textCss)
            script(
                src = "{{ url_for('static', filename='ace_editor/src/ace.js') }}"
            ) {}
        } else {
            link(rel = "stylesheet", href = "${resourceRoot}ace_editor/css/ace.css", type = LinkType.textCss)
            script(
                src = "${resourceRoot}ace_editor/src/ace.js"
            ) {}
        }
    }

}