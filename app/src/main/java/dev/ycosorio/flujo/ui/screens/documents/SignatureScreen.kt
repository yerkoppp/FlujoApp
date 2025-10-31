package dev.ycosorio.flujo.ui.screens.documents

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import dev.ycosorio.flujo.ui.components.SignatureCanvas
import dev.ycosorio.flujo.ui.components.StrokePath
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SignatureScreen(
    viewModel: SignatureViewModel = hiltViewModel(),
    onSignatureSaved: () -> Unit,
    onBackPressed: () -> Unit
) {
    val paths = remember { mutableStateListOf<StrokePath>() }
    val captureController = rememberCaptureController()
    val signatureState by viewModel.signatureState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(signatureState) {
        if (signatureState is Resource.Success) {
            onSignatureSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firmar Documento") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { paths.clear() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, "Limpiar")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Limpiar")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                // 1. Llama a la función. Si falla, salta al 'catch'.
                                val imageBitmap: ImageBitmap = captureController.captureAsync().await()

                                // 2. Si tiene éxito, convierte y guarda.
                                val androidBitmap = imageBitmap.asAndroidBitmap()
                                viewModel.saveSignature(androidBitmap)

                            } catch (e: Exception) {
                                // 3. Aquí manejas el error de captura
                                // ej: viewModel.showError("Error al capturar firma")
                            }
                        }
                    },
                    enabled = paths.isNotEmpty() && signatureState !is Resource.Loading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Done, "Guardar")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Guardar Firma")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (signatureState is Resource.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (signatureState is Resource.Error) {
                Text(
                    (signatureState as Resource.Error).message ?: "Error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            // El lienzo para firmar
            SignatureCanvas(
                paths = paths,
                onPathDrawn = {
                    paths.add(it)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray) // Mantenemos el fondo gris
                    .capturable(controller = captureController) // <- El modificador v3.x
            )
        }
    }
}