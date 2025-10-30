package dev.ycosorio.flujo.ui.screens.documents

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
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shreyaspatil.capturable.Capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import dev.ycosorio.flujo.ui.components.SignatureCanvas
import dev.ycosorio.flujo.ui.components.StrokePath
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.launch
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(
    viewModel: SignatureViewModel = viewModel(),
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
                            val bitmapResult = captureController.captureAsBitmap()
                            viewModel.saveSignature(bitmapResult)
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
            Capturable(
                controller = captureController,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray),
                onCaptured = {}// Un fondo para el área de captura
            ) {
                SignatureCanvas(
                    paths = paths,
                    onPathDrawn = {
                        paths.add(it)
                    }
                )
            }
        }
    }
}