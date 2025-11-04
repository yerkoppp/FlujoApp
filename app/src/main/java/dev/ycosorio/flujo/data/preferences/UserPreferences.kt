package dev.ycosorio.flujo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class FontScale {
    SMALL,    // 0.85
    NORMAL,   // 1.0
    LARGE,    // 1.15
    XLARGE    // 1.30
}

enum class ContrastLevel {
    STANDARD,
    MEDIUM,
    HIGH
}

data class UserPreferencesData(
    val theme: AppTheme = AppTheme.SYSTEM,
    val fontScale: FontScale = FontScale.NORMAL,
    val contrastLevel: ContrastLevel = ContrastLevel.STANDARD
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val CONTRAST_LEVEL = stringPreferencesKey("contrast_level")
    }

    val userPreferences: Flow<UserPreferencesData> = context.dataStore.data
        .map { preferences ->
            UserPreferencesData(
                theme = AppTheme.valueOf(
                    preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
                ),
                fontScale = FontScale.valueOf(
                    preferences[PreferencesKeys.FONT_SCALE] ?: FontScale.NORMAL.name
                ),
                contrastLevel = ContrastLevel.valueOf(
                    preferences[PreferencesKeys.CONTRAST_LEVEL] ?: ContrastLevel.STANDARD.name
                )
            )
        }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun updateFontScale(scale: FontScale) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SCALE] = scale.name
        }
    }

    suspend fun updateContrastLevel(level: ContrastLevel) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTRAST_LEVEL] = level.name
        }
    }
}