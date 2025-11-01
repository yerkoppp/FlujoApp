package dev.ycosorio.flujo.ui.screens.worker.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.domain.model.User


@Composable
fun WorkerDashboard(
    user: User,
    navController: NavHostController
) {
    // Aquí irán los componentes específicos para el trabajador
    // Por ejemplo, documentos pendientes de firma, estado de su última solicitud, etc.
    Text("Vista del Trabajador", style = MaterialTheme.typography.bodyLarge)
}