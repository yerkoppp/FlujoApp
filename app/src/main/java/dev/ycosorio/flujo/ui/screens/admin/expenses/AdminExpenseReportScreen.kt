package dev.ycosorio.flujo.ui.screens.admin.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ycosorio.flujo.domain.model.ExpenseReport
import dev.ycosorio.flujo.domain.model.ExpenseReportStatus
import dev.ycosorio.flujo.utils.Resource
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExpenseReportScreen(
    onNavigateBack: () -> Unit,
    onReportClick: (String) -> Unit,
    viewModel: AdminExpenseReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expenseReports by viewModel.expenseReports.collectAsStateWithLifecycle()
    val selectedWorker by viewModel.selectedWorker.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()

    var showWorkerFilter by remember { mutableStateOf(false) }
    var showStatusFilter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rendiciones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filtros
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Filtros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Filtro por trabajador
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showWorkerFilter = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                selectedWorker?.let { workerId ->
                                    uiState.workers.find { it.uid == workerId }?.name ?: "Trabajador"
                                } ?: "Todos los trabajadores"
                            )
                        }

                        if (selectedWorker != null) {
                            IconButton(onClick = { viewModel.onWorkerFilterChanged(null) }) {
                                Icon(Icons.Default.Close, "Limpiar filtro")
                            }
                        }
                    }

                    // Filtro por estado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStatusFilter = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                selectedStatus?.let { status ->
                                    when (status) {
                                        ExpenseReportStatus.DRAFT -> "Borrador"
                                        ExpenseReportStatus.SUBMITTED -> "Enviado"
                                        ExpenseReportStatus.APPROVED -> "Aprobado"
                                        ExpenseReportStatus.REJECTED -> "Rechazado"
                                    }
                                } ?: "Todos los estados"
                            )
                        }

                        if (selectedStatus != null) {
                            IconButton(onClick = { viewModel.onStatusFilterChanged(null) }) {
                                Icon(Icons.Default.Close, "Limpiar filtro")
                            }
                        }
                    }
                }
            }

            // Lista de rendiciones
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                when (val state = expenseReports) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(modifier = Modifier)
                    }
                    is Resource.Success -> {
                        if (state.data.isNullOrEmpty()) {
                            Text(
                                "No hay rendiciones",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = state.data,
                                    key = { it.id }
                                ) { report ->
                                    AdminExpenseReportCard(
                                        report = report,
                                        onClick = { onReportClick(report.id) },
                                        onApprove = {
                                            viewModel.approveReport(report.id)
                                        },
                                        onReject = { notes ->
                                            viewModel.rejectReport(report.id, notes)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        Text(
                            state.message ?: "Error",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    // Diálogo filtro trabajador
    if (showWorkerFilter) {
        AlertDialog(
            onDismissRequest = { showWorkerFilter = false },
            title = { Text("Filtrar por trabajador") },
            text = {
                LazyColumn {
                    item {
                        TextButton(
                            onClick = {
                                viewModel.onWorkerFilterChanged(null)
                                showWorkerFilter = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Todos los trabajadores")
                        }
                    }
                    items(uiState.workers) { worker ->
                        TextButton(
                            onClick = {
                                viewModel.onWorkerFilterChanged(worker.uid)
                                showWorkerFilter = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(worker.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWorkerFilter = false }) {
                    Text("CERRAR")
                }
            }
        )
    }

    // Diálogo filtro estado
    if (showStatusFilter) {
        AlertDialog(
            onDismissRequest = { showStatusFilter = false },
            title = { Text("Filtrar por estado") },
            text = {
                Column {
                    listOf(
                        null to "Todos los estados",
                        ExpenseReportStatus.SUBMITTED to "Enviado",
                        ExpenseReportStatus.APPROVED to "Aprobado",
                        ExpenseReportStatus.REJECTED to "Rechazado"
                    ).forEach { (status, label) ->
                        TextButton(
                            onClick = {
                                viewModel.onStatusFilterChanged(status)
                                showStatusFilter = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusFilter = false }) {
                    Text("CERRAR")
                }
            }
        )
    }

    // Mostrar errores
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun AdminExpenseReportCard(
    report: ExpenseReport,
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
    var showRejectDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = report.workerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(report.createdDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${report.items.size} comprobante(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusChip(status = report.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total: ${currencyFormat.format(report.totalAmount)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Botones de acción (solo si está SUBMITTED)
            if (report.status == ExpenseReportStatus.SUBMITTED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RECHAZAR")
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("APROBAR")
                    }
                }
            }
        }
    }

    // Diálogo para rechazar con nota
    if (showRejectDialog) {
        var rejectNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Rechazar rendición") },
            text = {
                Column {
                    Text("Ingresa el motivo del rechazo:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectNotes,
                        onValueChange = { rejectNotes = it },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (rejectNotes.isNotBlank()) {
                            onReject(rejectNotes)
                            showRejectDialog = false
                        }
                    },
                    enabled = rejectNotes.isNotBlank()
                ) {
                    Text("RECHAZAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }
}

@Composable
fun StatusChip(status: ExpenseReportStatus) {
    val (text, color) = when (status) {
        ExpenseReportStatus.DRAFT -> "Borrador" to MaterialTheme.colorScheme.secondary
        ExpenseReportStatus.SUBMITTED -> "Enviado" to MaterialTheme.colorScheme.primary
        ExpenseReportStatus.APPROVED -> "Aprobado" to MaterialTheme.colorScheme.tertiary
        ExpenseReportStatus.REJECTED -> "Rechazado" to MaterialTheme.colorScheme.error
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}