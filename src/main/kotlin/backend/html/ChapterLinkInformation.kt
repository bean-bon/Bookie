package backend.html

data class ChapterLinkInformation(
    val previousInfo: ChapterInformation?,
    val currentInfo: ChapterInformation,
    val nextInfo: ChapterInformation?,
) {
    companion object {
        val empty = ChapterLinkInformation(null, ChapterInformation.empty, null)
    }
}
