package views.helpers

sealed interface SystemUtils {
    companion object {
        fun getPlatform(): String = System.getProperty("os.name")
        fun isMacOS(): Boolean {
            val platform = getPlatform()
            return "Mac" in platform
        }
    }
}