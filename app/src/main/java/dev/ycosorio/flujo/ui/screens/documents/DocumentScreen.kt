package dev.ycosorio.flujo.ui.screens.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ycosorio.flujo.domain.model.*
import dev.ycosorio.flujo.utils.Resource
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DocumentScreen(
    viewModel: DocumentViewModel = viewModel() // Hilt proveerá el VM
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
                    when (user.role) {
                        Role.ADMINISTRADOR -> AdminDocumentScreen(viewModel)
                        Role.TRABAJADOR -> WorkerDocumentScreen(viewModel)
                    }
                } else {
                    Text("Error: Usuario no encontrado.")
                }
            }
            is Resource.Error -> Text(text = state.message ?: "Error al cargar el usuario.")
        }
    }
}

// --- VISTA PARA EL ADMINISTRADOR ---
@Composable
private fun AdminDocumentScreen(viewModel: DocumentViewModel) {
    val templatesState by viewModel.templates.collectAsState()

    Column {
        Text("Admin: Plantillas de Documentos", style = MaterialTheme.typography.headlineSmall)

        // Aquí iría la UI para asignar plantillas, etc.
        when (val state = templatesState) {
            is Resource.Loading, is Resource.Idle -> CircularProgressIndicator()
            is Resource.Success -> {
                LazyColumn {
                    items(state.data ?: emptyList()) { template ->
                        // UI simple por ahora
                        Text(template.title, modifier = Modifier.padding(8.dp))
                    }
                }
            }
            is Resource.Error -> Text(state.message ?: "Error")
        }
    }
}

// --- VISTA PARA EL TRABAJADOR ---
@Composable
private fun WorkerDocumentScreen(viewModel: DocumentViewModel) {
    val pendingState by viewModel.pendingAssignments.collectAsState()

    Column {
        Text("Mis Documentos Pendientes", style = MaterialTheme.typography.headlineSmall)

        when (val state = pendingState) {
            is Resource.Loading, is Resource.Idle -> CircularProgressIndicator()
            is Resource.Success -> {
                val assignments = state.data
                if (assignments.isNullOrEmpty()) {
                    Text("No tienes documentos pendientes de firma.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(assignments) { assignment ->
                            DocumentAssignmentItem(assignment = assignment)
                        }
                    }
                }
            }
            is Resource.Error -> Text(state.message ?: "Error")
        }
    }
}

// --- COMPONENTE REUTILIZABLE PARA UN ITEM DE ASIGNACIÓN ---
@Composable
fun DocumentAssignmentItem(assignment: DocumentAssignment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(assignment.documentTitle, fontWeight = FontWeight.Bold)
                Text(
                    "Asignado: ${assignment.assignedDate.toFormattedString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Mostramos el botón solo si está pendiente
            if (assignment.status == DocumentStatus.PENDIENTE) {
                Button(onClick = { /* TODO: Navegar a la pantalla de firma */ }) {
                    Text("Firmar")
                }
            } else {
                Text("Firmado", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// Función de ayuda para formatear la fecha
private fun Date.toFormattedString(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(this)
}