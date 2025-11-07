package dev.ycosorio.flujo.ui.screens.admin.vehicles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.model.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleManagementScreen(
    navController: NavController,
    viewModel: VehicleManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados para controlar los diálogos
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Vehicle?>(null) }
    var showAssignDialog by remember { mutableStateOf<Vehicle?>(null) }

    // Escucha los eventos (Snackbars)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is VehicleManagementViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Vehículos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Crear Vehículo")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            VehicleManagementContent(
                state = uiState,
                onDeleteClick = { vehicle -> showDeleteDialog = vehicle },
                onAssignClick = { vehicle -> showAssignDialog = vehicle },
                onRemoveUserClick = { userId, vehicleId ->
                    viewModel.removeUserFromVehicle(userId, vehicleId)
                }
            )

            // Indicador de carga centralizado
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
            }
        }
    }

    // --- DIÁLOGOS ---

    // Diálogo para CREAR vehículo
    if (showCreateDialog) {
        CreateVehicleDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { plate, description ->
                viewModel.createVehicle(plate, description)
                showCreateDialog = false
            }
        )
    }

    // Diálogo para ASIGNAR usuario
    showAssignDialog?.let { vehicle ->
        AssignUserDialog(
            vehicle = vehicle,
            unassignedWorkers = uiState.unassignedWorkers,
            onDismiss = { showAssignDialog = null },
            onAssignUser = { userId ->
                viewModel.assignUserToVehicle(userId, vehicle.id)
                showAssignDialog = vehicle
            }
        )
    }

    // Diálogo para CONFIRMAR ELIMINACIÓN
    showDeleteDialog?.let { vehicle ->
        ConfirmDeleteDialog(
            vehicle = vehicle,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                viewModel.deleteVehicle(vehicle.id)
                showDeleteDialog = null
            }
        )
    }
}

@Composable
private fun VehicleManagementContent(
    state: VehicleManagementUiState,
    onDeleteClick: (Vehicle) -> Unit,
    onAssignClick: (Vehicle) -> Unit,
    onRemoveUserClick: (userId: String, vehicleId: String) -> Unit
) {
    if (state.vehicles.isEmpty() && !state.isLoading) {
        Text(
            text = "No hay vehículos creados.\nPresiona (+) para añadir uno.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.vehicles, key = { it.id }) { vehicle ->
                VehicleItemCard(
                    vehicle = vehicle,
                    allWorkers = state.allWorkers,
                    onDeleteClick = { onDeleteClick(vehicle) },
                    onAssignClick = { onAssignClick(vehicle) },
                    onRemoveUserClick = { userId ->
                        onRemoveUserClick(userId, vehicle.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun VehicleItemCard(
    vehicle: Vehicle,
    allWorkers: List<User>,
    onDeleteClick: () -> Unit,
    onAssignClick: () -> Unit,
    onRemoveUserClick: (userId: String) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- Fila Superior: Info y Botón de Eliminar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = vehicle.plate,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = vehicle.description.ifEmpty { "Sin descripción" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar Vehículo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Sección de Usuarios Asignados ---
            Text(
                text = "Usuarios Asignados (${vehicle.userIds.size} / ${vehicle.maxUsers})",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (vehicle.userIds.isEmpty()) {
                Text(
                    text = "Sin usuarios asignados.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    vehicle.userIds.forEach { userId ->
                        val userName = allWorkers.find { it.uid == userId }?.name ?: "Usuario (ID: $userId)"
                        AssignedUserRow(
                            userName = userName,
                            onRemove = { onRemoveUserClick(userId) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Botón de Asignar ---
            Button(
                onClick = onAssignClick,
                enabled = vehicle.userIds.size < vehicle.maxUsers,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Asignar Usuario")
            }
        }
    }
}

@Composable
private fun AssignedUserRow(
    userName: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = userName, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remover usuario",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// --- DIÁLOGOS COMPOSABLES ---

@Composable
private fun CreateVehicleDialog(
    onDismiss: () -> Unit,
    onCreate: (plate: String, description: String) -> Unit
) {
    var plate by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var plateError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nuevo Vehículo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = plate,
                    onValueChange = {
                        plate = it.uppercase()
                        plateError = null
                    },
                    label = { Text("Patente (Ej: ABCD12)") },
                    isError = plateError != null,
                    supportingText = { if (plateError != null) Text(plateError!!) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (Ej: Camioneta Roja)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (plate.isBlank()) {
                        plateError = "La patente es obligatoria"
                    } else {
                        onCreate(plate, description)
                    }
                }
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignUserDialog(
    vehicle: Vehicle,
    unassignedWorkers: List<User>,
    onDismiss: () -> Unit,
    onAssignUser: (userId: String) -> Unit
) {
    // Usamos un Dialog básico para poder poner un LazyColumn dentro
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Asignar a ${vehicle.plate}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (unassignedWorkers.isEmpty()) {
                    Text(
                        text = "No hay usuarios disponibles para asignar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp) // Altura fija para el scroll
                    ) {
                        items(unassignedWorkers, key = { it.uid }) { user ->
                            TextButton(
                                onClick = { onAssignUser(user.uid) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(user.name)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text("¿Eliminar Vehículo?") },
        text = {
            Text(
                "Estás a punto de eliminar el vehículo con patente ${vehicle.plate}.\n\n" +
                        "Esta acción también des-asignará a los ${vehicle.userIds.size} usuarios asociados. ¿Estás seguro?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}