package dev.ycosorio.flujo.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.utils.Resource

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onUserAuthorized: (User) -> Unit, // Callback para notificar que el usuario es válido
    onUserUnauthorized: () -> Unit // Callback para expulsar al usuario
) {
    val userState by viewModel.userState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = userState) {
            is Resource.Idle, is Resource.Loading -> {
                CircularProgressIndicator()
                Text("Verificando acceso...", modifier = Modifier.padding(top = 16.dp))
            }
            is Resource.Success -> {
                val user = state.data
                if (user != null) {
                    // ¡Éxito! El usuario existe en Firestore.
                    // Usamos LaunchedEffect para notificar a MainScreen que cargue el dashboard real.
                    LaunchedEffect(user) {
                        onUserAuthorized(user)
                    }
                } else {
                    // Esto no debería pasar si Success tiene datos, pero es un buen seguro.
                    UnauthorizedContent(onSignOut = onUserUnauthorized)
                }
            }

            is Resource.Error -> {
                Text(text = state.message ?: "Error al cargar el usuario.")
                UnauthorizedContent(onSignOut = onUserUnauthorized)
            }
        }
    }
}

@Composable
private fun UnauthorizedContent(onSignOut: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Acceso Denegado",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "No tienes permiso para acceder a esta aplicación. " +
                    "Contacta al administrador para que apruebe tu cuenta.",
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Button(onClick = onSignOut) {
            Text("Cerrar Sesión")
        }
    }
}
