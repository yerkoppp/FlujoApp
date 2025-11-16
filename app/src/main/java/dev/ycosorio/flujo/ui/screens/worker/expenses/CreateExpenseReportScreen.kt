package dev.ycosorio.flujo.ui.screens.worker.expenses

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import dev.ycosorio.flujo.domain.model.ExpenseItem
import dev.ycosorio.flujo.domain.model.ExpenseReportStatus
import dev.ycosorio.flujo.utils.Resource
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseReportScreen(
    reportId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: ExpenseReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(reportId) {
        viewModel.loadReport(reportId)
    }

    LaunchedEffect(uiState.reportSubmittedSuccessfully) {
        if (uiState.reportSubmittedSuccessfully) {
            viewModel.resetSubmittedFlag()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            reportId == null -> "Nueva Rendición"
                            uiState.currentReport?.status == ExpenseReportStatus.DRAFT -> "Editar Rendición"
                            else -> "Ver Rendición"
                        }
                    )
                        },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (uiState.currentReport?.status == ExpenseReportStatus.DRAFT) {
                        TextButton(onClick = { viewModel.submitReport() }) {
                            Text("ENVIAR")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.currentReport != null &&
                uiState.currentReport?.status == ExpenseReportStatus.DRAFT) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Agregar Comprobante")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.currentReport != null -> {
                    val report = uiState.currentReport!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Total
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Total Acumulado",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    NumberFormat.getCurrencyInstance(Locale("es", "CL"))
                                        .format(report.totalAmount),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${report.items.size} comprobante(s)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // Lista de items
                        if (report.items.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay comprobantes agregados\nToca el botón + para agregar",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(report.items) { item ->
                                    ExpenseItemCard(
                                        item = item,
                                        onDelete = { viewModel.removeExpenseItem(item.id) },
                                        canDelete = report.status == ExpenseReportStatus.DRAFT
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        "Cargando...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Overlay de carga al subir imagen
            if (uiState.isUploadingImage) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(uiState.uploadProgress ?: "Procesando...")
                    }
                }
            }
        }
    }

    // Dialog para agregar comprobante
    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { uri, reason, docNumber, amount ->
                viewModel.addExpenseItem(uri, reason, docNumber, amount)
                showAddDialog = false
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
fun ExpenseItemCard(
    item: ExpenseItem,
    onDelete: () -> Unit,
    canDelete: Boolean = true
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagen
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "Comprobante",
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )

            // Detalles
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.reason,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Doc: ${item.documentNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = currencyFormat.format(item.amount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = dateFormat.format(item.uploadDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón eliminar
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (Uri, String, String, Double) -> Unit
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var reason by remember { mutableStateOf("") }
    var documentNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    // URI temporal para la foto de cámara
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Estado para disparar el lanzamiento de la cámara
    var shouldLaunchCamera by remember { mutableStateOf(false) }

    // Launcher para tomar foto con cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { selectedImageUri = it }
        }
        shouldLaunchCamera = false
    }

    // Launcher para solicitar permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, preparar y marcar para lanzar la cámara
            tempCameraUri = createTempImageUri(context)
            shouldLaunchCamera = true
        }
    }

    // Launcher para seleccionar imagen de galería
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }

    // Efecto para lanzar la cámara cuando el estado cambie
    LaunchedEffect(shouldLaunchCamera) {
        if (shouldLaunchCamera && tempCameraUri != null) {
            cameraLauncher.launch(tempCameraUri!!)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Comprobante") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botones para seleccionar origen de imagen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Cámara
                    OutlinedButton(
                        onClick = {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            tempCameraUri = createTempImageUri(context)
                            cameraLauncher.launch(tempCameraUri!!)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Cámara")
                    }

                    // Botón Galería
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Galería")
                    }
                }

                // Indicador de imagen seleccionada
                if (selectedImageUri != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Imagen seleccionada")
                        }
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo") },
                    placeholder = { Text("Ej: Combustible, Estacionamiento") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = documentNumber,
                    onValueChange = { documentNumber = it },
                    label = { Text("Nro Documento") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val uri = selectedImageUri
                    val amt = amount.toDoubleOrNull()
                    if (uri != null && reason.isNotBlank() &&
                        documentNumber.isNotBlank() && amt != null && amt > 0) {
                        onAdd(uri, reason, documentNumber, amt)
                    }
                },
                enabled = selectedImageUri != null && reason.isNotBlank() &&
                        documentNumber.isNotBlank() && amount.toDoubleOrNull() != null
            ) {
                Text("AGREGAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}

// Función auxiliar para crear URI (ahora fuera del @Composable)
private fun createTempImageUri(context: Context): Uri {
    // Crear directorio si no existe
    val cameraDir = File(context.cacheDir, "camera")
    if (!cameraDir.exists()) {
        cameraDir.mkdirs()
    }

    // Crear archivo temporal
    val tempFile = File.createTempFile(
        "expense_${System.currentTimeMillis()}",
        ".jpg",
        cameraDir
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}