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
            StatusBadge(status = request.status)

            // Mostramos los botones solo si la solicitud está pendiente
            if (request.status == RequestStatus.PENDIENTE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if(role == Role.ADMINISTRADOR) {
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
                    if(role == Role.TRABAJADOR) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancelar")
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: RequestStatus) {
    val backgroundColor = when (status) {
        RequestStatus.PENDIENTE -> MaterialTheme.colorScheme.tertiaryContainer
        RequestStatus.APROBADO -> Color(0xFFE8F5E9) // Verde claro
        RequestStatus.RECHAZADO -> MaterialTheme.colorScheme.errorContainer
        RequestStatus.RETIRADO -> Color(0xFFE1F5FE) // Azul claro
    }
    val contentColor = when (status) {
        RequestStatus.PENDIENTE -> MaterialTheme.colorScheme.onTertiaryContainer
        RequestStatus.APROBADO -> Color(0xFF1B5E20) // Verde oscuro
        RequestStatus.RECHAZADO -> MaterialTheme.colorScheme.onErrorContainer
        RequestStatus.RETIRADO -> Color(0xFF01579B) // Azul oscuro
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