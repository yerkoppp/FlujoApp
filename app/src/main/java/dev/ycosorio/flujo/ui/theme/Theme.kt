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
    onPrimary = FlujoSurfaceLight, // Blanco
    primaryContainer = FlujoPrimaryBlueVariant,
    onPrimaryContainer = FlujoSurfaceLight, // Blanco

    secondary = FlujoSecondaryCyan,
    onSecondary = FlujoSurfaceLight,
    secondaryContainer = FlujoSecondaryCyanVariant,
    onSecondaryContainer = FlujoSurfaceLight,

    tertiary = FlujoPrimaryBlue,
    onTertiary = FlujoSurfaceLight,
    tertiaryContainer = FlujoPrimaryBlueVariant,
    onTertiaryContainer = FlujoSurfaceLight,

    background = FlujoBackgroundLight, // Fondo gris (EEEEEE)
    onBackground = FlujoTextDark,

    surface = FlujoSurfaceLight, // Superficie blanca (FFFFFF)
    onSurface = FlujoTextDark,
    surfaceVariant = FlujoSurfaceVariantLight, // Variante gris (E0E0E0)
    onSurfaceVariant = FlujoTextMedium,

    error = FlujoErrorRed,
    onError = FlujoSurfaceLight,
    errorContainer = FlujoErrorRed,
    onErrorContainer = FlujoSurfaceLight,

    outline = FlujoTextLight,
)

// --- Definición del esquema de color OSCURO (DarkColorScheme) ---
private val DarkColorScheme = darkColorScheme(
    primary = FlujoDarkPrimary,
    onPrimary = FlujoDarkOnPrimary,
    primaryContainer = FlujoDarkPrimaryContainer,
    onPrimaryContainer = FlujoDarkTextPrimary,

    secondary = FlujoDarkSecondary,
    onSecondary = FlujoDarkOnSecondary,
    secondaryContainer = FlujoDarkSecondaryContainer,
    onSecondaryContainer = FlujoDarkTextPrimary,

    tertiary = FlujoDarkSecondary,
    onTertiary = FlujoDarkOnSecondary,
    tertiaryContainer = FlujoDarkSecondaryContainer,
    onTertiaryContainer = FlujoDarkTextPrimary,

    background = FlujoDarkBackground,
    onBackground = FlujoDarkTextPrimary,

    surface = FlujoDarkSurface,
    onSurface = FlujoDarkTextPrimary,
    surfaceVariant = FlujoDarkSurfaceVariant,
    onSurfaceVariant = FlujoDarkTextSecondary,

    error = FlujoErrorRed,
    onError = FlujoOnError,
    errorContainer = FlujoErrorRed,
    onErrorContainer = FlujoOnError,

    outline = FlujoDarkTextDisabled,
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
        typography = typography,
        shapes = Shapes,
        content = content
    )
}

private fun getLightColorScheme(contrastLevel: ContrastLevel): ColorScheme {
    return when (contrastLevel) {
        ContrastLevel.STANDARD -> LightColorScheme // Ya tiene más contraste
        ContrastLevel.MEDIUM -> LightColorScheme.copy(
            primary = FlujoPrimaryBlueVariant, // Primario más oscuro
            onPrimary = FlujoWhite,
            surfaceVariant = Color(0xFFBDBDBD) // Gris de tarjetas más oscuro
        )
        ContrastLevel.HIGH -> LightColorScheme.copy(
            primary = FlujoBlack,
            onPrimary = FlujoWhite,
            background = FlujoWhite,
            onBackground = FlujoBlack,
            surface = FlujoWhite,
            onSurface = FlujoBlack,
            onSurfaceVariant = FlujoBlack,
            outline = FlujoBlack
        )
    }
}

// --- LÓGICA DE CONTRASTE TEMA OSCURO ---
private fun getDarkColorScheme(contrastLevel: ContrastLevel): ColorScheme {
    return when (contrastLevel) {
        // Estándar: Fondo gris oscuro, Primario cian brillante
        ContrastLevel.STANDARD -> DarkColorScheme

        // Medio: Fondo negro puro, Primario azul oscuro (más contraste)
        ContrastLevel.MEDIUM -> DarkColorScheme.copy(
            primary = FlujoDarkPrimaryMedium, // Azul oscuro
            onPrimary = FlujoDarkOnPrimaryMedium, // Texto blanco
            background = FlujoBlack // Fondo negro
        )

        // Alto: Fondo negro puro, Primario blanco puro
        ContrastLevel.HIGH -> DarkColorScheme.copy(
            primary = FlujoWhite,
            onPrimary = FlujoBlack,
            background = FlujoBlack,
            onBackground = FlujoWhite,
            surface = FlujoBlack,
            onSurface = FlujoWhite,
            onSurfaceVariant = FlujoWhite,
            outline = FlujoWhite
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
