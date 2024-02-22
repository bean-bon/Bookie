package backend.html.helpers

/**
 * Keep track of objects which have already been defined
 */
internal object GenerationTracker {
    private val recognised: MutableSet<String> = mutableSetOf()
    fun addToList(path: String): Boolean = recognised.add(path)
    fun reset() = recognised.clear()
}