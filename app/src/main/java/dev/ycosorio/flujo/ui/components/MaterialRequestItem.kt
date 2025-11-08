package dev.ycosorio.flujo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.model.Role
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MaterialRequestItem(
    role: Role,
    request: MaterialRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDeliver: () -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${request.workerName} solicita ${request.quantity} x ${request.materialName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fecha: ${request.requestDate.toFormattedString()}",
                style = MaterialTheme.typography.bodySmall
            )

            // Mostrar notas del admin si existen
            if (!request.adminNotes.isNullOrBlank()) {
                Text(
                    text = "Notas: ${request.adminNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(status = request.status)
            // ✅ BOTONES SEGÚN ROL Y ESTADO
            when {
                // --- ADMIN ---
                role == Role.ADMINISTRADOR -> {
                    when (request.status) {
                        RequestStatus.PENDIENTE -> {
                            // Admin puede Aprobar o Rechazar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                Button(
                                    onClick = onReject,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Rechazar")
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Rechazar")
                                }
                                Button(onClick = onApprove) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Aprobar")
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Aprobar")
                                }
                            }
                        }
                        RequestStatus.APROBADO -> {
                            // Admin puede marcar como Entregado
                            Button(
                                onClick = onDeliver,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Entregar")
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("Marcar como Entregado")
                            }
                        }
                        else -> {
                            // Estados finales: RECHAZADO, ENTREGADO, CANCELADO
                            // No hay acciones disponibles
                        }
                    }
                }

                // --- TRABAJADOR ---
                role == Role.TRABAJADOR -> {
                    if (request.status == RequestStatus.PENDIENTE) {
                        // Trabajador puede cancelar si está pendiente
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancelar")
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Cancelar Solicitud")
                        }
                    }
                    // Si no está pendiente, no hay acciones
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: RequestStatus) {
    val (backgroundColor, contentColor) = when (status) {
        RequestStatus.PENDIENTE ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        RequestStatus.APROBADO ->
            Color(0xFFE8F5E9) to Color(0xFF1B5E20) // Verde claro/oscuro
        RequestStatus.RECHAZADO ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        RequestStatus.ENTREGADO ->
            Color(0xFFE1F5FE) to Color(0xFF01579B) // Azul claro/oscuro
        RequestStatus.CANCELADO ->
            Color(0xFFEEEEEE) to Color(0xFF616161) // Gris claro/oscuro
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor
    ) {
        Text(
            text = status.name,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun Date.toFormattedString(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(this)
}