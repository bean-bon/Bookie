package backend.html

/**
 * String wrapper for storing chapter names to use when
 * exporting a project.
 * @param path The relative path of the chapter being referenced.
 * @author Benjamin Groom
 */
data class ChapterInformation(
    val path: String,
    val name: String,
    val index: Int,
    val templateMarker: String
) {
    companion object {
        val empty = ChapterInformation("", "", -1,"")
    }
}