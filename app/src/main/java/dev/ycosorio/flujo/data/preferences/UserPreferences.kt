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

// Instancia de DataStore para las preferencias del usuario.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Define los temas de la aplicación disponibles.
 */
enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

/**
 * Define las escalas de fuente para la accesibilidad.
 */
enum class FontScale {
    SMALL,    // 0.85
    NORMAL,   // 1.0
    LARGE,    // 1.15
    XLARGE    // 1.30
}

/**
 * Define los niveles de contraste para la accesibilidad.
 */
enum class ContrastLevel {
    STANDARD,
    MEDIUM,
    HIGH
}

/**
 * Representa los datos de las preferencias del usuario.
 *
 * @property theme El tema actual de la aplicación.
 * @property fontScale La escala de fuente actual.
 * @property contrastLevel El nivel de contraste actual.
 */
data class UserPreferencesData(
    val theme: AppTheme = AppTheme.SYSTEM,
    val fontScale: FontScale = FontScale.NORMAL,
    val contrastLevel: ContrastLevel = ContrastLevel.STANDARD
)


/**
 * Repositorio para gestionar las preferencias del usuario utilizando Jetpack DataStore.
 * Proporciona métodos para leer y actualizar las preferencias de la aplicación.
 *
 * @param context El contexto de la aplicación, inyectado por Hilt.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val CONTRAST_LEVEL = stringPreferencesKey("contrast_level")
    }

    /**
     * Un [Flow] que emite los datos de las preferencias del usuario cada vez que cambian.
     */
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

    /**
     * Actualiza el tema de la aplicación en las preferencias.
     *
     * @param theme El nuevo tema a establecer.
     */
    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    /**
     * Actualiza la escala de la fuente en las preferencias.
     *
     * @param scale La nueva escala de fuente a establecer.
     */
    suspend fun updateFontScale(scale: FontScale) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SCALE] = scale.name
        }
    }

    /**
     * Actualiza el nivel de contraste en las preferencias.
     *
     * @param level El nuevo nivel de contraste a establecer.
     */
    suspend fun updateContrastLevel(level: ContrastLevel) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTRAST_LEVEL] = level.name
        }
    }
}
