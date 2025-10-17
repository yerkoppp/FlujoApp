package dev.ycosorio.flujo.ui.screens.worker.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.ui.components.MaterialRequestItem // Reutilizamos el componente del admin
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerRequestScreen(
    viewModel: WorkerRequestViewModel,
    onAddRequestClicked: () -> Unit
) {
    val uiState by viewModel.myRequestsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mis Solicitudes de Materiales") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRequestClicked) {
                Icon(Icons.Default.Add, contentDescription = "Crear nueva solicitud")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is Resource.Loading -> CircularProgressIndicator()
                is Resource.Success -> {
                    if (state.data.isNullOrEmpty()) {
                        Text("No has realizado ninguna solicitud.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(state.data) { request ->
                                // Reutilizamos el mismo componente, pero sin los botones de acción
                                MaterialRequestItem(
                                    request = request,
                                    onApprove = {}, // No hace nada en la vista del trabajador
                                    onReject = {}   // No hace nada en la vista del trabajador
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