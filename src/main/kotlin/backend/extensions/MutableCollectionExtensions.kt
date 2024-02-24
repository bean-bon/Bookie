package backend.extensions

fun <T> MutableCollection<T>.removeFirstWhere(predicate: (T) -> Boolean) {
    val value = find(predicate)
    value?.let { remove(it) }
}