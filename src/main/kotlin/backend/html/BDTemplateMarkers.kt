package backend.html

/**
 * Object for templating markers used in the included
 * HTML template.
 * @author Benjamin Groom
 */
object BDTemplateMarkers {
    const val title = "%%page_title"
    const val headScripts = "%%script_imports"
    const val toc = "%%table_of_contents"
    const val body = "%%bd_blocks"
    const val scripts = "%%scripts"
}