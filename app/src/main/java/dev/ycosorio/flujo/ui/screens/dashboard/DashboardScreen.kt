package dev.ycosorio.flujo.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    viewModel: DashboardViewModel
) {
    val userState by viewModel.userState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val state = userState) {
            is Resource.Idle, is Resource.Loading -> CircularProgressIndicator()
            is Resource.Success -> {
                val user = state.data
                if (user != null) {
                    DashboardContent(user = user)
                } else {
                    Text("No se pudo cargar la información del usuario.")
                }
            }
            is Resource.Error -> Text(text = state.message ?: "Error al cargar el usuario.")
        }
    }
}

@Composable
private fun DashboardContent(user: User) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¡Hola, ${user.name}!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Bienvenido a Flujo",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // El contenido del dashboard cambia según el rol del usuario
        when (user.role) {
            Role.ADMINISTRADOR -> AdminDashboard()
            Role.TRABAJADOR -> WorkerDashboard()
        }
    }
}

@Composable
private fun AdminDashboard() {
    // Aquí irían los componentes específicos para el administrador
    // Por ejemplo, un resumen de solicitudes pendientes, usuarios activos, etc.
    Text("Vista del Administrador", style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun WorkerDashboard() {
    // Aquí irían los componentes específicos para el trabajador
    // Por ejemplo, documentos pendientes de firma, estado de su última solicitud, etc.
    Text("Vista del Trabajador", style = MaterialTheme.typography.bodyLarge)
}