package dev.ycosorio.flujo.ui.screens.admin.users

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.utils.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    user: User,
    deleteUserState: Resource<Unit>,
    onBackPressed: () -> Unit,
    onEditClicked: (User) -> Unit,
    onDeleteClicked: (User) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Trabajador") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClicked(user) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información personal
            SectionTitle("Información Personal")
            InfoField("Nombre", user.name)
            InfoField("Email", user.email)
            user.phoneNumber?.let { InfoField("Teléfono", it) }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Información laboral
            SectionTitle("Información Laboral")
            InfoField("Cargo", user.position)
            InfoField("Área", user.area)
            InfoField("Rol", user.role.name)

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Fechas de contrato
            SectionTitle("Contrato")
            InfoField("Inicio", user.contractStartDate.toFormattedString())
            user.contractEndDate?.let {
                InfoField("Término", it.toFormattedString())
            } ?: InfoField("Término", "Indefinido")

            // Asignaciones (si existen)
            if (user.assignedVehicleId != null || user.assignedPhoneId != null || user.assignedPcId != null) {
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                SectionTitle("Asignaciones")
                user.assignedVehicleId?.let { InfoField("Vehículo", it) }
                user.assignedPhoneId?.let { InfoField("Teléfono", it) }
                user.assignedPcId?.let { InfoField("PC", it) }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (deleteUserState !is Resource.Loading) {
                    showDeleteDialog = false
                }
            },
            title = { Text("Eliminar Trabajador") },
            text = {
                when (deleteUserState) {
                    is Resource.Loading -> Text("Eliminando usuario...")
                    is Resource.Error -> Text("Error: ${deleteUserState.message}\n\n¿Deseas intentar de nuevo?")
                    else -> Text("¿Estás seguro de que deseas eliminar a ${user.name}? Esta acción no se puede deshacer.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteUserState !is Resource.Loading) {
                            onDeleteClicked(user)
                        }
                    },
                    enabled = deleteUserState !is Resource.Loading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (deleteUserState is Resource.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (deleteUserState is Resource.Error) "Reintentar" else "Eliminar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = deleteUserState !is Resource.Loading
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun InfoField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun Date.toFormattedString(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(this)
}