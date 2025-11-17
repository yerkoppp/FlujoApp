package dev.ycosorio.flujo.ui.screens.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignDocumentScreen(
    viewModel: DocumentViewModel = hiltViewModel(),
    templateId: String,
    onBackPressed: () -> Unit,
    onAssignSuccess: () -> Unit
) {
    // 1. Estados de la UI
    val templatesState by viewModel.templates.collectAsState()
    val workersState by viewModel.allWorkers.collectAsState()
    val assignmentState by viewModel.assignmentState.collectAsState()

    // 2. Estado para la selección
    var selectedWorkerIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 3. Encontrar la plantilla seleccionada
    val selectedTemplate = (templatesState as? Resource.Success)?.data?.find { it.id == templateId }

    // 4. Efecto para manejar el resultado de la asignación
    LaunchedEffect(assignmentState) {
        if (assignmentState is Resource.Success) {
            viewModel.resetAssignmentState()
            onAssignSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asignar: ${selectedTemplate?.title ?: "..."}") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 5. Lista de Trabajadores
            when (val state = workersState) {
                is Resource.Loading, is Resource.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Resource.Success -> {
                    val allWorkers = state.data ?: emptyList()
                    val allSelected = allWorkers.isNotEmpty() && selectedWorkerIds.size == allWorkers.size

                    Column(modifier = Modifier.weight(1f)) {
                        // Checkbox para seleccionar/deseleccionar todos
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 16.dp, 16.dp, 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = allSelected,
                                    onCheckedChange = { checked ->
                                        selectedWorkerIds = if (checked) {
                                            allWorkers.map { it.uid }.toSet()
                                        } else {
                                            emptySet()
                                        }
                                    }
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    if (allSelected) "Deseleccionar todos" else "Seleccionar todos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            items(
                                items = allWorkers
                            ) { user ->
                                WorkerCheckItem(
                                    user = user,
                                    isSelected = selectedWorkerIds.contains(user.uid),
                                    onCheckChanged = {
                                        selectedWorkerIds = if (selectedWorkerIds.contains(user.uid)) {
                                            selectedWorkerIds - user.uid
                                        } else {
                                            selectedWorkerIds + user.uid
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> Text(state.message ?: "Error cargando trabajadores")
            }

            // 6. Botón de Asignar
            if (assignmentState is Resource.Error) {
                Text(
                    (assignmentState as Resource.Error).message ?: "Error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Button(
                onClick = {
                    if (selectedTemplate != null) {
                        viewModel.assignDocumentToWorkers(selectedTemplate, selectedWorkerIds.toList())
                    }
                },
                enabled = assignmentState !is Resource.Loading && selectedWorkerIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (assignmentState is Resource.Loading) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else {
                    Text("Asignar a ${selectedWorkerIds.size} trabajador(es)")
                }
            }
        }
    }
}

// Composable de ayuda para la fila del trabajador con Checkbox
@Composable
private fun WorkerCheckItem(
    user: User,
    isSelected: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckChanged
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(user.name, style = MaterialTheme.typography.bodyLarge)
            Text(user.position, style = MaterialTheme.typography.bodySmall)
        }
    }
}