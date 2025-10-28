package dev.ycosorio.flujo.ui.screens.admin.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.ui.components.MaterialRequestItem
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialRequestScreen(
    viewModel: MaterialRequestViewModel
) {
    val uiState by viewModel.requestsState.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

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
            FilterBar(
                selectedStatus = statusFilter,
                onFilterChanged = { newStatus ->
                    viewModel.onStatusFilterChanged(newStatus)
                }
            )

            // --- Contenido Principal ---
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is Resource.Idle, is Resource.Loading -> CircularProgressIndicator()
                    is Resource.Success -> {
                        if (state.data.isNullOrEmpty()) {
                            Text("No hay solicitudes para mostrar.")
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                items(state.data) { request ->
                                    MaterialRequestItem(
                                        request = request,
                                        onApprove = { viewModel.updateRequestStatus(request.id, RequestStatus.APROBADO) },
                                        onReject = { viewModel.updateRequestStatus(request.id, RequestStatus.RECHAZADO) }
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