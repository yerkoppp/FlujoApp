package dev.ycosorio.flujo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material3.Typography
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // Asegúrate de importar Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.ycosorio.flujo.data.preferences.AppTheme
import dev.ycosorio.flujo.data.preferences.ContrastLevel
import dev.ycosorio.flujo.data.preferences.FontScale

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
    appTheme: AppTheme = AppTheme.SYSTEM,
    fontScale: FontScale = FontScale.NORMAL,
    contrastLevel: ContrastLevel = ContrastLevel.STANDARD,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Determinar si usar tema oscuro
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    // Seleccionar esquema de colores según contraste
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorScheme(contrastLevel)
        else -> getLightColorScheme(contrastLevel)
    }

    // Aplicar escalado de fuente
    val typography = getScaledTypography(fontScale)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

private fun getLightColorScheme(contrastLevel: ContrastLevel): ColorScheme {
    return when (contrastLevel) {
        ContrastLevel.STANDARD -> LightColorScheme
        ContrastLevel.MEDIUM -> LightColorScheme.copy(
            primary = FlujoPrimaryBlueVariant,
            onPrimary = FlujoSurfaceWhite,
            surface = FlujoBackgroundLight
        )
        ContrastLevel.HIGH -> LightColorScheme.copy(
            primary = FlujoPrimaryBlueVariant,
            onPrimary = FlujoSurfaceWhite,
            surface = FlujoSurfaceWhite,
            onSurface = FlujoTextDark,
            outline = FlujoTextDark
        )
    }
}

private fun getDarkColorScheme(contrastLevel: ContrastLevel): ColorScheme {
    return when (contrastLevel) {
        ContrastLevel.STANDARD -> DarkColorScheme
        ContrastLevel.MEDIUM -> DarkColorScheme.copy(
            primary = FlujoDarkPrimary,
            onPrimary = FlujoDarkOnPrimary
        )
        ContrastLevel.HIGH -> DarkColorScheme.copy(
            primary = FlujoDarkPrimary,
            onPrimary = FlujoDarkBackground,
            surface = FlujoDarkBackground,
            onSurface = FlujoDarkTextPrimary,
            outline = FlujoDarkTextPrimary
        )
    }
}

private fun getScaledTypography(fontScale: FontScale): Typography {
    val scale = when (fontScale) {
        FontScale.SMALL -> 0.85f
        FontScale.NORMAL -> 1.0f
        FontScale.LARGE -> 1.15f
        FontScale.XLARGE -> 1.30f
    }

    return Typography.copy(
        displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize * scale),
        displayMedium = Typography.displayMedium.copy(fontSize = Typography.displayMedium.fontSize * scale),
        displaySmall = Typography.displaySmall.copy(fontSize = Typography.displaySmall.fontSize * scale),
        headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * scale),
        headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * scale),
        headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize * scale),
        titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * scale),
        titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * scale),
        titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize * scale),
        bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * scale),
        bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * scale),
        bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize * scale),
        labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * scale),
        labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * scale),
        labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize * scale)
    )
}
