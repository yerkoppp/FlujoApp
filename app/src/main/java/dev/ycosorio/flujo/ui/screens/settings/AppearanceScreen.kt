package dev.ycosorio.flujo.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.ycosorio.flujo.data.preferences.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    viewModel: AppearanceViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val preferences by viewModel.preferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apariencia") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Tema
            PreferenceSection(
                title = "TEMA",
                description = "Cambia entre tema claro, oscuro o automático según el sistema"
            ) {
                AppTheme.values().forEach { theme ->
                    PreferenceItem(
                        title = when (theme) {
                            AppTheme.LIGHT -> "Claro"
                            AppTheme.DARK -> "Oscuro"
                            AppTheme.SYSTEM -> "Sistema"
                        },
                        selected = preferences.theme == theme,
                        onClick = { viewModel.updateTheme(theme) }
                    )
                }
            }

            HorizontalDivider()

            // Tamaño de fuente
            PreferenceSection(
                title = "TAMAÑO DE TEXTO",
                description = "Ajusta el tamaño del texto en toda la app"
            ) {
                FontScale.values().forEach { scale ->
                    PreferenceItem(
                        title = when (scale) {
                            FontScale.SMALL -> "Pequeño"
                            FontScale.NORMAL -> "Normal"
                            FontScale.LARGE -> "Grande"
                            FontScale.XLARGE -> "Extra Grande"
                        },
                        selected = preferences.fontScale == scale,
                        onClick = { viewModel.updateFontScale(scale) }
                    )
                }
            }

            HorizontalDivider()

            // Contraste
            PreferenceSection(
                title = "CONTRASTE",
                description = "Aumenta el contraste para mejorar la legibilidad"
            ) {
                ContrastLevel.values().forEach { level ->
                    PreferenceItem(
                        title = when (level) {
                            ContrastLevel.STANDARD -> "Estándar"
                            ContrastLevel.MEDIUM -> "Medio"
                            ContrastLevel.HIGH -> "Alto"
                        },
                        selected = preferences.contrastLevel == level,
                        onClick = { viewModel.updateContrastLevel(level) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferenceItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selected) {
                RadioButton(
                    selected = true,
                    onClick = null
                )
            }
        }
    }
}