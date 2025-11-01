package dev.ycosorio.flujo.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.firebase.ui.auth.AuthUI
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.components.UserAvatar
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onNavigateToAuth: () -> Unit // Para cerrar sesión
) {
    val userState by viewModel.userState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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
            // --- Cabecera con datos del usuario ---
            item {
                when (val state = userState) {
                    is Resource.Success -> {
                        state.data?.let { user ->
                            ProfileHeader(user = user)
                        }
                    }
                    is Resource.Loading -> CircularProgressIndicator()
                    else -> Text("No se pudo cargar el usuario.")
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // --- Opciones ---
            item {
                SettingsItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Editar Perfil",
                    onClick = { /* TODO: Navegar a Editar Perfil */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Tema y Apariencia",
                    onClick = { /* TODO: Navegar a Tema */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    onClick = { /* TODO: Navegar a Notificaciones */ }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // --- Cerrar Sesión ---
            item {
                Button(
                    onClick = {
                        AuthUI.getInstance()
                            .signOut(context)
                            .addOnCompleteListener { onNavigateToAuth() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Cerrar Sesión")
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: User) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        UserAvatar(user = user) // Reutilizamos el avatar
        Spacer(Modifier.height(8.dp))
        Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(user.email, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
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
            Text(title, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}