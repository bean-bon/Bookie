package backend.html.helpers

/**
 * Helper object for creating new HTML ids.
 */
internal object IDCreator {

    val codeBlock = IDBuilder("codeblock")
    val paragraph = IDBuilder("")
    val chapter = IDBuilder("chapter")
    val inlineBlock = IDBuilder("inline")
    val figure = IDBuilder("")

    fun resetCounters() {
        codeBlock.resetCounter()
        paragraph.resetCounter()
        chapter.resetCounter()
        inlineBlock.resetCounter()
        figure.resetCounter()
    }

}

internal data class IDBuilder(
    private val prefix: String,
    private var current: Int = 0
) {
    val nextId: String
        get() = run {
            current += 1
            if (prefix.isNotEmpty()) "${prefix}_${current}"
            else current.toString()
        }
    val currentIndex: Int
        get() = current
    fun resetCounter() { current = 0 }
}
