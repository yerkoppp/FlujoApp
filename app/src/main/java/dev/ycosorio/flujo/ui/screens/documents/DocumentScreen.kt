package dev.ycosorio.flujo.ui.screens.documents

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
                    TemplatesTab(
                        templatesState = templatesState,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                1 -> {
                    // Pestaña de Asignaciones
                    AssignmentsTab(
                        assignmentsState = assignmentsState,
                        navController = navController
                    )
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
        TabRow(selectedTabIndex = selectedTab) {
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

// --- TAB DE ASIGNACIONES CON FILTROS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentsTab(
    assignmentsState: Resource<List<DocumentAssignment>>,
    navController: NavHostController
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<DocumentStatus?>(null) }
    var selectedWorkerName by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf<DateFilter>(DateFilter.ALL) }

    // Diálogo de filtros
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filtrar asignaciones") },
            text = {
                Column {
                    Text("Estado:", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { selectedStatus = null },
                            label = { Text("Todos") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedStatus == DocumentStatus.PENDIENTE,
                            onClick = { selectedStatus = DocumentStatus.PENDIENTE },
                            label = { Text("Pendientes") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedStatus == DocumentStatus.FIRMADO,
                            onClick = { selectedStatus = DocumentStatus.FIRMADO },
                            label = { Text("Firmados") }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("Fecha:", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = selectedDateFilter == DateFilter.ALL,
                            onClick = { selectedDateFilter = DateFilter.ALL },
                            label = { Text("Todas") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedDateFilter == DateFilter.LAST_7_DAYS,
                            onClick = { selectedDateFilter = DateFilter.LAST_7_DAYS },
                            label = { Text("7 días") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedDateFilter == DateFilter.LAST_30_DAYS,
                            onClick = { selectedDateFilter = DateFilter.LAST_30_DAYS },
                            label = { Text("30 días") }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("Trabajador:", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = selectedWorkerName,
                        onValueChange = { selectedWorkerName = it },
                        placeholder = { Text("Nombre del trabajador") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedStatus = null
                    selectedWorkerName = ""
                    selectedDateFilter = DateFilter.ALL
                    showFilterDialog = false
                }) {
                    Text("Limpiar")
                }
            }
        )
    }

    when (val state = assignmentsState) {
        is Resource.Loading, is Resource.Idle -> CircularProgressIndicator()
        is Resource.Success -> {
            val allAssignments = state.data ?: emptyList()

            // Aplicar filtros
            val filteredAssignments = allAssignments.filter { assignment ->
                val statusMatch = selectedStatus == null || assignment.status == selectedStatus
                val workerMatch = selectedWorkerName.isBlank() ||
                    assignment.workerName.contains(selectedWorkerName, ignoreCase = true)
                val dateMatch = when (selectedDateFilter) {
                    DateFilter.ALL -> true
                    DateFilter.LAST_7_DAYS -> {
                        val sevenDaysAgo = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -7)
                        }.time
                        assignment.assignedDate.after(sevenDaysAgo)
                    }
                    DateFilter.LAST_30_DAYS -> {
                        val thirtyDaysAgo = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -30)
                        }.time
                        assignment.assignedDate.after(thirtyDaysAgo)
                    }
                }
                statusMatch && workerMatch && dateMatch
            }

            Column {
                // Botón de filtros y resumen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredAssignments.size} de ${allAssignments.size} asignaciones",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FilledTonalButton(
                        onClick = { showFilterDialog = true }
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar")
                        Spacer(Modifier.width(4.dp))
                        Text("Filtros")
                    }
                }

                // Chips de filtros activos
                if (selectedStatus != null || selectedWorkerName.isNotBlank() || selectedDateFilter != DateFilter.ALL) {
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedStatus != null) {
                            AssistChip(
                                onClick = { selectedStatus = null },
                                label = { Text("Estado: ${selectedStatus?.name}") },
                                trailingIcon = { Text("×") }
                            )
                        }
                        if (selectedWorkerName.isNotBlank()) {
                            AssistChip(
                                onClick = { selectedWorkerName = "" },
                                label = { Text("Trabajador: $selectedWorkerName") },
                                trailingIcon = { Text("×") }
                            )
                        }
                        if (selectedDateFilter != DateFilter.ALL) {
                            AssistChip(
                                onClick = { selectedDateFilter = DateFilter.ALL },
                                label = { Text(selectedDateFilter.label) },
                                trailingIcon = { Text("×") }
                            )
                        }
                    }
                }

                if (filteredAssignments.isEmpty()) {
                    Text("No hay asignaciones que coincidan con los filtros.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredAssignments, key = { it.id }) { assignment ->
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
        }
        is Resource.Error -> Text(state.message ?: "Error al cargar asignaciones")
    }
}

// Enum para filtros de fecha
private enum class DateFilter(val label: String) {
    ALL("Todas"),
    LAST_7_DAYS("Últimos 7 días"),
    LAST_30_DAYS("Últimos 30 días")
}

// --- TAB DE PLANTILLAS CON SWIPE Y ABRIR DOCUMENTO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesTab(
    templatesState: Resource<List<DocumentTemplate>>,
    viewModel: DocumentViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val deleteState by viewModel.deleteState.collectAsState()
    var templateToDelete by remember { mutableStateOf<DocumentTemplate?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Manejar resultado de eliminación
    LaunchedEffect(deleteState) {
        if (deleteState is Resource.Success) {
            templateToDelete = null
            showDeleteDialog = false
            viewModel.resetDeleteState()
        }
    }

    // Diálogo de confirmación
    if (showDeleteDialog && templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar \"${templateToDelete?.title}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        templateToDelete?.let { viewModel.deleteTemplate(it) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    when (val state = templatesState) {
        is Resource.Loading, is Resource.Idle -> CircularProgressIndicator()
        is Resource.Success -> {
            if (state.data.isNullOrEmpty()) {
                Text("No hay plantillas de documentos.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = state.data,
                        key = { it.id }
                    ) { template ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    templateToDelete = template
                                    showDeleteDialog = true
                                    false // No descartar automáticamente
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = Color.White
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            template.title,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    // Botón para ver/abrir documento
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(Uri.parse(template.fileUrl), "application/pdf")
                                                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = "Ver documento",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    // Botón para asignar
                                    IconButton(
                                        onClick = {
                                            navController.navigate(Routes.AssignDocument.createRoute(template.id))
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Asignar"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mostrar error si hay uno en deleteState
            if (deleteState is Resource.Error) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text((deleteState as Resource.Error).message ?: "Error al eliminar")
                }
            }
        }
        is Resource.Error -> Text(state.message ?: "Error")
    }
}

// Función de ayuda para formatear la fecha
private fun Date.toFormattedString(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(this)
}