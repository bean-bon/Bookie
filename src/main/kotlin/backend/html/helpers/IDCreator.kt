package backend.html.helpers

/**
 * Helper object for creating new HTML ids.
 */
internal object IDCreator {

    val codeBlock = IDBuilder("code_block")
    val paragraph = IDBuilder("paragraph")
    val chapter = IDBuilder("chapter")
    val inlineBlock = IDBuilder("inline")
    val answer = IDBuilder("answer")
    val figure = IDBuilder("figure")

    fun resetCounters() {
        codeBlock.resetCounter()
        paragraph.resetCounter()
        chapter.resetCounter()
        inlineBlock.resetCounter()
        figure.resetCounter()
        Headings.resetAllFrom(1)
    }

    internal object Headings {

        val h1 = IDBuilder("h1")
        val h2 = IDBuilder("h2")
        val h3 = IDBuilder("h3")
        private val h4 = IDBuilder("h4")
        private val h5 = IDBuilder("h5")
        private val h6 = IDBuilder("h6")

        fun getBuilderFor(index: Int): IDBuilder? = when (index) {
            1 -> h1
            2 -> h2
            3 -> h3
            4 -> h4
            5 -> h5
            6 -> h6
            else -> null
        }

        fun resetAllFrom(level: Int) {
            for (i in level..6) getBuilderFor(i)?.resetCounter()
        }

    }

    fun makeSectionStringForCurrentState(): String {
        var heading = "${Headings.h1.currentIndex}."
        if (Headings.h2.currentIndex == 0) return heading
        heading += "${Headings.h2.currentIndex}."
        if (Headings.h3.currentIndex == 0) return heading
        return heading + "${Headings.h3.currentIndex}."
    }

    fun makeFigureNumberForCurrentState(): String =
        makeSectionStringForCurrentState() + figure.currentIndex

    fun resetFigureIfNeeded(level: Int) {
        if (level < 4) figure.resetCounter()
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
