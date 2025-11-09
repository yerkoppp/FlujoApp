package dev.ycosorio.flujo.ui.screens.documents

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.ycosorio.flujo.domain.model.DocumentStatus
import dev.ycosorio.flujo.utils.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    assignmentId: String,
    viewModel: DocumentViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onNavigateToSignature: () -> Unit
) {
    val pendingAssignments by viewModel.pendingAssignments.collectAsState()
    val context = LocalContext.current

    // Buscar el assignment específico
    val assignment = (pendingAssignments as? Resource.Success)?.data?.find { it.id == assignmentId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Documento") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (assignment == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título
                Text(
                    text = assignment.documentTitle,
                    style = MaterialTheme.typography.headlineMedium
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                // Información del documento
                InfoRow(label = "Estado", value = assignment.status.name)
                InfoRow(
                    label = "Fecha de asignación",
                    value = assignment.assignedDate.toFormattedString()
                )

                if (assignment.status == DocumentStatus.FIRMADO && assignment.signedDate != null) {
                    InfoRow(
                        label = "Fecha de firma",
                        value = assignment.signedDate.toFormattedString()
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botón Ver Documento
                Button(
                    onClick = {
                        // Abrir PDF con app externa
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(assignment.documentFileUrl), "application/pdf")
                            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }

                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Si no hay app para abrir PDFs, mostrar error
                            android.widget.Toast.makeText(
                                context,
                                "No se encontró una aplicación para abrir PDFs",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ver Documento")
                }

                // Botón Firmar (solo si está pendiente)
                if (assignment.status == DocumentStatus.PENDIENTE) {
                    Button(
                        onClick = onNavigateToSignature,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Firmar Documento")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Este documento ya ha sido firmado",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun Date.toFormattedString(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(this)
}