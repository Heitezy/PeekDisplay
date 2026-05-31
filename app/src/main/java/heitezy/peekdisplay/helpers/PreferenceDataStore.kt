package heitezy.peekdisplay.helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "${context.packageName}_preferences"))
    },
)

internal class PreferenceDataStore(context: Context) {
    private val dataStore = context.dataStore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _preferenceFlow = MutableStateFlow<Preferences?>(null)
    val preferenceFlow: StateFlow<Preferences?> = _preferenceFlow.asStateFlow()

    init {
        scope.launch {
            dataStore.data.collect {
                _preferenceFlow.value = it
            }
        }
    }

    private fun getPreferencesSnapshot(): Preferences {
        val current = _preferenceFlow.value
        if (current != null) return current

        return runBlocking { dataStore.data.first() }
    }

    fun getString(key: String, default: String): String =
        getPreferencesSnapshot()[stringPreferencesKey(key)] ?: default

    fun getBoolean(key: String, default: Boolean): Boolean =
        getPreferencesSnapshot()[booleanPreferencesKey(key)] ?: default

    fun getInt(key: String, default: Int): Int =
        getPreferencesSnapshot()[intPreferencesKey(key)] ?: default

    fun getStringSet(key: String, default: Set<String>): Set<String> =
        getPreferencesSnapshot()[stringSetPreferencesKey(key)] ?: default

    fun getAll(): Map<String, Any?> =
        getPreferencesSnapshot().asMap().mapKeys { it.key.name }

    // Reactive Flows for Compose
    fun getStringFlow(key: String, default: String): Flow<String> =
        dataStore.data.map { it[stringPreferencesKey(key)] ?: default }

    fun getBooleanFlow(key: String, default: Boolean): Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(key)] ?: default }

    fun getIntFlow(key: String, default: Int): Flow<Int> =
        dataStore.data.map { it[intPreferencesKey(key)] ?: default }

    fun getStringSetFlow(key: String, default: Set<String>): Flow<Set<String>> =
        dataStore.data.map { it[stringSetPreferencesKey(key)] ?: default }

    fun getChangeFlow(): Flow<Unit> = dataStore.data.map { }

    fun edit(block: Editor.() -> Unit) {
        val editor = Editor()
        block(editor)
        editor.apply()
    }

    inner class Editor {
        private val changes = mutableListOf<suspend (MutablePreferences) -> Unit>()

        fun putString(key: String, value: String): Editor {
            changes.add { it[stringPreferencesKey(key)] = value }
            return this
        }

        fun putBoolean(key: String, value: Boolean): Editor {
            changes.add { it[booleanPreferencesKey(key)] = value }
            return this
        }

        fun putInt(key: String, value: Int): Editor {
            changes.add { it[intPreferencesKey(key)] = value }
            return this
        }

        fun putStringSet(key: String, value: Set<String>): Editor {
            changes.add { it[stringSetPreferencesKey(key)] = value }
            return this
        }

        fun apply() {
            scope.launch {
                dataStore.edit { preferences ->
                    changes.forEach { it(preferences) }
                }
            }
        }
    }
}
