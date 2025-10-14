package dev.ycosorio.flujo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color // Asegúrate de importar Color

// --- Definición del esquema de color CLARO (LightColorScheme) ---
private val LightColorScheme = lightColorScheme(
    primary = FlujoPrimaryBlue,
    onPrimary = FlujoSurfaceWhite,
    primaryContainer = FlujoPrimaryBlueVariant,
    onPrimaryContainer = FlujoSurfaceWhite,

    secondary = FlujoSecondaryCyan,
    onSecondary = FlujoSurfaceWhite,
    secondaryContainer = FlujoSecondaryCyanVariant,
    onSecondaryContainer = FlujoSurfaceWhite,

    tertiary = FlujoPrimaryBlue, // Puedes ajustar o añadir un tercer color si es necesario
    onTertiary = FlujoSurfaceWhite,
    tertiaryContainer = FlujoPrimaryBlueVariant,
    onTertiaryContainer = FlujoSurfaceWhite,

    background = FlujoBackgroundLight,
    onBackground = FlujoTextDark,

    surface = FlujoSurfaceWhite,
    onSurface = FlujoTextDark,
    surfaceVariant = FlujoBackgroundLight, // Usamos el color de fondo para una variante de superficie sutil
    onSurfaceVariant = FlujoTextMedium,

    error = FlujoErrorRed, // Usando el rojo oscuro para tema claro
    onError = FlujoSurfaceWhite,
    errorContainer = FlujoErrorRed,
    onErrorContainer = FlujoSurfaceWhite,

    outline = FlujoTextLight, // Bordes o divisores
    // Añadir otros colores si son necesarios para M3 como scrim, inversePrimary, etc.
)

// --- Definición del esquema de color OSCURO (DarkColorScheme) ---
private val DarkColorScheme = darkColorScheme(
    primary = FlujoDarkPrimary, // Cian brillante como Primary en oscuro
    onPrimary = FlujoDarkOnPrimary, // Texto oscuro sobre el primary oscuro
    primaryContainer = FlujoDarkPrimaryContainer,
    onPrimaryContainer = FlujoDarkTextPrimary, // Texto claro sobre el contenedor primario

    secondary = FlujoDarkSecondary, // Azul claro como Secondary en oscuro
    onSecondary = FlujoDarkOnSecondary, // Texto oscuro sobre el secondary oscuro
    secondaryContainer = FlujoDarkSecondaryContainer,
    onSecondaryContainer = FlujoDarkTextPrimary, // Texto claro sobre el contenedor secundario

    tertiary = FlujoDarkSecondary, // Puedes usar secondary o un tercer color si es necesario
    onTertiary = FlujoDarkOnSecondary,
    tertiaryContainer = FlujoDarkSecondaryContainer,
    onTertiaryContainer = FlujoDarkTextPrimary,

    background = FlujoDarkBackground,
    onBackground = FlujoDarkTextPrimary, // Texto claro sobre el fondo oscuro

    surface = FlujoDarkSurface,
    onSurface = FlujoDarkTextPrimary, // Texto claro sobre las superficies oscuras
    surfaceVariant = FlujoDarkSurfaceVariant, // Variante de superficie ligeramente diferente
    onSurfaceVariant = FlujoDarkTextSecondary, // Texto secundario sobre la variante de superficie

    error = FlujoErrorRed, // El rojo de error ajustado para oscuro (definido en Color.kt)
    onError = FlujoOnError, // Texto negro sobre el error rojo
    errorContainer = FlujoErrorRed,
    onErrorContainer = FlujoOnError,

    outline = FlujoDarkTextDisabled, // Bordes o divisores con un tono más suave
)


@Composable
fun FlujoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Esto es para Material You en Android 12+,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
         dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // Opcional: Material You
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
         }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
