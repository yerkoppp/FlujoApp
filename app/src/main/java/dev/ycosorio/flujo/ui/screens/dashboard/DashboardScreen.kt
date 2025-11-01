package dev.ycosorio.flujo.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthUI
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
    val context = LocalContext.current
    // Cerrar sesión automáticamente cuando se muestra esta pantalla
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000) // Pequeño delay para que se vea el mensaje
        onSignOut()
        AuthUI.getInstance()
            .signOut(context)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))

        Text(
            "⚠️ Acceso Denegado",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Tu cuenta no está registrada en el sistema.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Spacer(Modifier.height(16.dp))

        Text(
            "Solo los usuarios autorizados por el administrador pueden acceder a esta aplicación.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text(
            "Contacta al administrador para solicitar acceso.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Cerrar Sesión")
        }
    }
}
