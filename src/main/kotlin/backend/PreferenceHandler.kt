package backend

import java.util.prefs.Preferences

object PreferenceHandler {

    private var userPreferences = Preferences.userRoot()
    private var preferenceSubscribers = mutableMapOf<String, MutableList<(String) -> Unit>>()

    init {
        // Set the system properties to allow selecting directories
        // in the AWT FileDialog.
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        userPreferences.addPreferenceChangeListener {
            preferenceSubscribers[it.key]?.forEach { func -> it?.newValue?.let(func) }
        }
    }

    fun readUserPreference(key: String): String? = userPreferences.get(key, null)

    fun setUserPreference(key: String, value: String) {
        userPreferences.put(key, value)
        syncPreferences()
    }

    fun clearPreference(key: String) {
        userPreferences.remove(key)
        syncPreferences()
    }

    /**
     * Recommended method for getting the most up-to-date
     * preference values, use if getting synchronisation issues.
     * @param key string corresponding to some preference
     * @param callback function to run with the new preference value on update
     */
    fun subscribeToPreferenceChange(key: String, callback: (String) -> Unit) {
        val entry = preferenceSubscribers[key]
        if (entry == null) {
            preferenceSubscribers[key] = mutableListOf(callback)
        } else {
            preferenceSubscribers[key]!!.add(callback)
        }
    }

    private fun syncPreferences() = userPreferences.flush()

}

fun PreferenceHandler.projectName(): String? =
    readUserPreference(PreferencePaths.user.lastProjectName)


object PreferencePaths {
    object user {
        const val lastProjectPath = "LAST_PROJECT_PATH_TO_ROOT"
        const val lastProjectName = "LAST_PROJECT_NAME"
    }

}