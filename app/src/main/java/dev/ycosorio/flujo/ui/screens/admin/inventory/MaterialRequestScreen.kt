package dev.ycosorio.flujo.ui.screens.admin.inventory

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.ui.components.MaterialRequestItem
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialRequestScreen(
    viewModel: MaterialRequestViewModel
) {
    val uiState by viewModel.requestsState.collectAsState()
    val currentFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    // --- Definimos las opciones para el filtro ---
    val filterOptions = listOf(
        null to "Todos",
        RequestStatus.PENDIENTE to "Pendientes",
        RequestStatus.APROBADO to "Aprobadas",
        RequestStatus.RECHAZADO to "Rechazadas",
        RequestStatus.ENTREGADO to "Entregadas",
        RequestStatus.CANCELADO to "Canceladas"
    )
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = filterOptions.find { it.first == currentFilter }?.second ?: "Todos"

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Solicitudes de Materiales") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Barra de Filtros ---
            /*   FilterBar(
                   selectedStatus = statusFilter,
                   onFilterChanged = { newStatus ->
                       viewModel.onStatusFilterChanged(newStatus)
                   }
               )*/

        /*    SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                filterOptions.forEachIndexed { index, (status, label) ->
                    SegmentedButton(
                        selected = currentFilter == status,
                        onClick = { viewModel.onStatusFilterChanged(status) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = filterOptions.size
                        ),
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }*/

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = "Filtrar: $selectedLabel",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Estado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filterOptions.forEach { (status, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.onStatusFilterChanged(status)
                                expanded = false
                            },
                            leadingIcon = if (currentFilter == status) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }

            // --- Contenido Principal ---
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (val state = uiState) {
                    is Resource.Idle, is Resource.Loading -> CircularProgressIndicator()
                    is Resource.Success -> {
                        if (state.data.isNullOrEmpty()) {
                            Text("No hay solicitudes para mostrar.", textAlign = TextAlign.Center)
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                items(state.data) { request ->
                                    MaterialRequestItem(
                                        role = Role.ADMINISTRADOR,
                                        request = request,
                                        onApprove = {
                                            viewModel.updateRequestStatus(
                                            request.id,
                                            RequestStatus.APROBADO
                                            )
                                        },
                                        onReject = {
                                            viewModel.updateRequestStatus(
                                            request.id,
                                            RequestStatus.RECHAZADO
                                            )
                                        },
                                        onDeliver = {
                                            viewModel.markAsDelivered(request)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Error -> Text(text = state.message ?: "Error desconocido")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    selectedStatus: RequestStatus?,
    onFilterChanged: (RequestStatus?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onFilterChanged(null) },
            label = { Text("Todos") }
        )
        RequestStatus.values().forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onFilterChanged(status) },
                label = { Text(status.name.replaceFirstChar { it.titlecase() }) }
            )
        }
    }
}