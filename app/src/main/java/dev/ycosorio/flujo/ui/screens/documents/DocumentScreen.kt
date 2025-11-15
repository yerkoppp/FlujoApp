package dev.ycosorio.flujo.ui.screens.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.ui.navigation.Routes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold

@Composable
fun DocumentScreen(
    viewModel: DocumentViewModel = hiltViewModel(),
    onNavigateToSignature: (String) -> Unit,
    navController: NavHostController
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
                        Role.ADMINISTRADOR -> AdminDocumentScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
                        Role.TRABAJADOR -> WorkerDocumentScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
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
private fun AdminDocumentScreen(
    viewModel: DocumentViewModel,
    navController: NavHostController
) {
    val templatesState by viewModel.templates.collectAsState()
    val assignmentsState by viewModel.allAssignments.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Routes.UploadTemplate.route)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Subir nueva plantilla")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Gestión de Documentos",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(16.dp))

            // TabRow para cambiar entre Plantillas y Asignaciones
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Plantillas") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Asignaciones") }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Contenido según la pestaña seleccionada
            when (selectedTab) {
                0 -> {
                    // Pestaña de Plantillas
                    when (val state = templatesState) {
                        is Resource.Loading, is Resource.Idle -> CircularProgressIndicator()
                        is Resource.Success -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.data ?: emptyList()) { template ->
                                    Card(
                                        onClick = {
                                            navController.navigate(Routes.AssignDocument.createRoute(template.id))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(template.title, modifier = Modifier.weight(1f))
                                            Icon(Icons.Default.ChevronRight, contentDescription = "Asignar")
                                        }
                                    }
                                }
                            }
                        }
                        is Resource.Error -> Text(state.message ?: "Error")
                    }
                }
                1 -> {
                    // Pestaña de Asignaciones
                    when (val state = assignmentsState) {
                        is Resource.Loading, is Resource.Idle -> CircularProgressIndicator()
                        is Resource.Success -> {
                            val assignments = state.data
                            if (assignments.isNullOrEmpty()) {
                                Text("No hay documentos asignados.")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(assignments) { assignment ->
                                        Card(
                                            onClick = {
                                            navController.navigate(Routes.DocumentDetail.createRoute(assignment.id))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    assignment.documentTitle,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Trabajador: ${assignment.workerName}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    "Asignado: ${assignment.assignedDate.toFormattedString()}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Estado: ${assignment.status.name}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (assignment.status == DocumentStatus.FIRMADO) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                    if (assignment.status == DocumentStatus.FIRMADO && assignment.signedDate != null) {
                                                        Text(
                                                            "Firmado: ${assignment.signedDate.toFormattedString()}",
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is Resource.Error -> Text(state.message ?: "Error al cargar asignaciones")
                    }
                }
            }
        }
    }
}

// --- VISTA PARA EL TRABAJADOR ---
@Composable
private fun WorkerDocumentScreen(
    viewModel: DocumentViewModel,
    navController: NavHostController
) {
    val pendingState by viewModel.pendingAssignments.collectAsState()
    val signedState by viewModel.signedAssignments.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Mis Documentos",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // TabRow para cambiar entre Pendientes y Firmados
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Pendientes") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Firmados") }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Contenido según la pestaña seleccionada
        when (selectedTab) {
            0 -> {
                // Pestaña de Pendientes
                when (val state = pendingState) {
                    is Resource.Loading, is Resource.Idle -> CircularProgressIndicator()
                    is Resource.Success -> {
                        val assignments = state.data
                        if (assignments.isNullOrEmpty()) {
                            Text("No tienes documentos pendientes de firma.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(
                                    items = assignments,
                                    key = { it.id }
                                ) { assignment ->
                                    DocumentAssignmentItem(
                                        assignment = assignment,
                                        navController = navController
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Error -> Text(state.message ?: "Error")
                }
            }
            1 -> {
                // Pestaña de Firmados
                when (val state = signedState) {
                    is Resource.Loading, is Resource.Idle -> CircularProgressIndicator()
                    is Resource.Success -> {
                        val assignments = state.data
                        if (assignments.isNullOrEmpty()) {
                            Text("No has firmado documentos aún.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(
                                    items = assignments,
                                    key = { it.id }
                                ) { assignment ->
                                    SignedDocumentItem(
                                        assignment = assignment,
                                        navController = navController
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Error -> Text(state.message ?: "Error al cargar documentos firmados")
                }
            }
        }
    }
}

// --- COMPONENTE REUTILIZABLE PARA UN ITEM DE ASIGNACIÓN ---
@Composable
fun DocumentAssignmentItem(
    assignment: DocumentAssignment,
    navController: NavHostController
) {
    Card(onClick = {
        navController.navigate(Routes.DocumentDetail.createRoute(assignment.id))
    },
        modifier = Modifier.fillMaxWidth()
    ) {
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
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Ver detalle",
                tint = MaterialTheme.colorScheme.primary
            )

        }
    }
}

// --- COMPONENTE PARA UN DOCUMENTO FIRMADO ---
@Composable
fun SignedDocumentItem(
    assignment: DocumentAssignment,
    navController: NavHostController
) {
    Card(
        onClick = {
            navController.navigate(Routes.DocumentDetail.createRoute(assignment.id))
        },
        modifier = Modifier.fillMaxWidth()
    ) {
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
                assignment.signedDate?.let { signedDate ->
                    Text(
                        "Firmado: ${signedDate.toFormattedString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Ver documento",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Función de ayuda para formatear la fecha
private fun Date.toFormattedString(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(this)
}