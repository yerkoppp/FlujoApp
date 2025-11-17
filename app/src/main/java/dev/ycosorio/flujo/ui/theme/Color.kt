package dev.ycosorio.flujo.ui.theme

import androidx.compose.ui.graphics.Color

// --- Colores base extraídos del logo y extendidos ---

// Colores primarios (azules del logo)
val FlujoPrimaryBlue = Color(0xFF1B6EB5) // Azul principal para tema claro
val FlujoPrimaryBlueVariant = Color(0xFF0D5A9C) // Azul más oscuro para tema claro

// Colores secundarios/acento (cian del logo)
val FlujoSecondaryCyan = Color(0xFF00B8D4) // Cian de acento para tema claro
val FlujoSecondaryCyanVariant = Color(0xFF0097A7) // Cian más oscuro para tema claro

val FlujoTerciaryBlueSky = Color(0xFFB2EBF2) // Cian de acento para tema claro
val FlujoTerciaryBlueSkyVariant = Color(0xFF80DEEA)
val FlujoTerciaryBlueSkyLight = Color(0xB2E0F7FA)

// ---  TEMA CLARO ---
// Neutros para tema CLARO (Ajustados para más contraste)

val FlujoSurfaceBlue = Color(0xFFE0F7FA) // Un cian muy claro, derivado de tu logo
val FlujoBackgroundLight = Color(0xFFEEEEEE) // Fondo gris (antes F0F2F5)
val FlujoSurfaceLight = Color(0xFFFFFFFF) // Superficie blanca (para las Cards)
val FlujoSurfaceVariantLight = Color(0xFFE0E0E0) // Variante de superficie (gris más oscuro)
val FlujoTextDark = Color(0xFF212121) // Gris oscuro para texto principal
val FlujoTextMedium = Color(0xFF616161) // Gris medio para texto secundario
val FlujoTextLight = Color(0xFFBDBDBD) // Gris claro para texto deshabilitado

// --- Colores específicos para TEMA OSCURO ---

val FlujoDarkBackground = Color(0xFF121212) // Fondo muy oscuro
val FlujoDarkSurface = Color(0xFF1E1E1E) // Superficies ligeramente menos oscuras que el fondo
val FlujoDarkSurfaceVariant = Color(0xFF2C2C2C) // Una variante de superficie para distinción
val FlujoDarkTextPrimary = Color(0xFFE0E0E0) // Gris muy claro para texto principal
val FlujoDarkTextSecondary = Color(0xFFB0B0B0) // Gris claro para texto secundario
val FlujoDarkTextDisabled = Color(0xFF6F6F6F) // Gris medio para texto deshabilitado

// Colores ESTÁNDAR para tema oscuro
val FlujoDarkPrimary = Color(0xFF00BCD4) // Cian brillante (Estándar)
val FlujoDarkOnPrimary = Color(0xFF212121) // Texto oscuro sobre este primary brillante
val FlujoDarkPrimaryContainer = Color(0xFF0097A7) // Versión más oscura del cian para contenedores

val FlujoDarkSecondary = Color(0xFF90CAF9) // Un azul claro complementario para acento en oscuro
val FlujoDarkOnSecondary = Color(0xFF212121) // Texto oscuro sobre este secondary
val FlujoDarkSecondaryContainer = Color(0xFF42A5F5) // Versión más oscura del azul claro

val FlujoErrorRed = Color(0xFFCF6679) // Rojo de error para tema oscuro (más suave)
val FlujoOnError = Color(0xFF000000) // Texto negro sobre el error rojo


// --- COLORES PARA CONTRASTE MEDIO (Oscuro) ---
// Usaremos el primario del tema claro (más oscuro) para aumentar contraste
val FlujoDarkPrimaryMedium = Color(0xFF1B6EB5) // Azul oscuro (en lugar de cian brillante)
val FlujoDarkOnPrimaryMedium = Color(0xFFFFFFFF) // Texto blanco

// --- COLORES PARA CONTRASTE ALTO (Genéricos) ---
val FlujoBlack = Color(0xFF000000)
val FlujoWhite = Color(0xFFFFFFFF)