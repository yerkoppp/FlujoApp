package dev.ycosorio.flujo.ui.screens.documents

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.ycosorio.flujo.utils.Resource
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadTemplateScreen(
    viewModel: DocumentViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onUploadSuccess: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val uploadState by viewModel.uploadState.collectAsState()

    // Launcher para el selector de archivos
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                Log.d("UploadTemplate", "📎 Archivo seleccionado: $it")
                selectedUri = it
            }
        }
    )

    // Launcher para permisos (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("UploadTemplate", "✅ Permiso concedido")
                filePickerLauncher.launch("application/pdf")
            } else {
                Log.w("UploadTemplate", "❌ Permiso denegado")
            }
        }
    )

    // Efecto para volver atrás si la subida es exitosa
    LaunchedEffect(uploadState) {
        if (uploadState is Resource.Success) {
            Log.d("UploadTemplate", "✅ Subida exitosa, regresando")
            viewModel.resetUploadState()
            onUploadSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subir Plantilla") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título del Documento") },
                placeholder = { Text("Ej: Contrato de Trabajo") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    Log.d("UploadTemplate", "🔘 Botón presionado")
                    // En Android 13+ necesitamos permiso
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        filePickerLauncher.launch("application/pdf")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    selectedUri?.lastPathSegment?.take(30) ?: "Seleccionar Archivo PDF"
                )
            }

            if (selectedUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "✓ Archivo seleccionado: ${selectedUri?.lastPathSegment}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (uploadState is Resource.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (uploadState as Resource.Error).message ?: "Error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Button(
                onClick = {
                    Log.d("UploadTemplate", "📤 Iniciando subida: $title")
                    viewModel.uploadTemplate(title, selectedUri)
                },
                enabled = uploadState !is Resource.Loading &&
                        title.isNotBlank() &&
                        selectedUri != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uploadState is Resource.Loading) {
                    CircularProgressIndicator(
                        Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Subiendo...")
                } else {
                    Text("Subir y Guardar Plantilla")
                }
            }
        }
    }
}