package dev.ycosorio.flujo.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ycosorio.flujo.BuildConfig
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.AppViewModel
import dev.ycosorio.flujo.ui.components.UserAvatar
import dev.ycosorio.flujo.utils.Resource
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAppearance: () -> Unit
) {
    val userState by viewModel.userState.collectAsState()
    val logoutState by viewModel.logoutState.collectAsState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Observar el estado de logout
    LaunchedEffect(logoutState) {
        when (logoutState) {
            is LogoutState.Success -> {
                Timber.d("✅ Navegando a login")
                onNavigateToAuth()
            }
            is LogoutState.Error -> {
                Timber.e("❌ Error en logout: ${(logoutState as LogoutState.Error).message}")
                Toast.makeText(
                    context,
                    "Error al cerrar sesión: ${(logoutState as LogoutState.Error).message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> { /* No hacer nada */ }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                when (val state = userState) {
                    is Resource.Success -> {
                        state.data?.let { user ->
                            ProfileHeader(
                                user = user,
                                onProfileClick = onNavigateToProfile
                            )
                        }
                    }
                    is Resource.Loading -> CircularProgressIndicator()
                    else -> Text("No se pudo cargar el usuario.")
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            // Apariencia
            item {
                Text(
                    "APARIENCIA",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Apariencia",
                    subtitle = "Tema y accesibilidad",
                    onClick = onNavigateToAppearance
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Cuenta
            item {
                Text(
                    "CUENTA",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Mi Perfil",
                    subtitle = "Ver y editar información personal",
                    onClick = onNavigateToProfile
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Acerca de
            item {
                Text(
                    "ACERCA DE",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Versión",
                    subtitle = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = {}
                )
            }

            item { Spacer(Modifier.height(32.dp)) }

            // Cerrar sesión
            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = logoutState !is LogoutState.Loading
                ) {
                    if (logoutState is LogoutState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Cerrando sesión...")
                    } else {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Cerrar Sesión")
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.performLogout(context, appViewModel)
                    }
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileHeader(user: User, onProfileClick: () -> Unit) {
    Card(
        onClick = onProfileClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(user = user)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    user.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(user.email, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}