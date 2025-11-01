package dev.ycosorio.flujo.ui.screens.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadTemplateScreen(
    viewModel: DocumentViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onUploadSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val uploadState by viewModel.uploadState.collectAsState()

    // Launcher para el selector de archivos
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            selectedUri = uri
        }
    )

    // Efecto para volver atrás si la subida es exitosa
    LaunchedEffect(uploadState) {
        if (uploadState is Resource.Success) {
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
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    // Pedimos cualquier tipo de documento, puedes ser más específico
                    filePickerLauncher.launch("*/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(selectedUri?.lastPathSegment ?: "Seleccionar Archivo")
            }

            Spacer(Modifier.weight(1f))

            if (uploadState is Resource.Error) {
                Text(
                    (uploadState as Resource.Error).message ?: "Error",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    viewModel.uploadTemplate(title, selectedUri)
                },
                enabled = uploadState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uploadState is Resource.Loading) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else {
                    Text("Subir y Guardar Plantilla")
                }
            }
        }
    }
}
