package dev.ycosorio.flujo.ui.screens.worker.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    viewModel: WorkerRequestViewModel,
    onSuccess: () -> Unit
) {
    // Estados para el formulario
    var materialName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    val createState by viewModel.createRequestState.collectAsState()

    // Efecto para volver atrás cuando la solicitud se crea con éxito
    LaunchedEffect(createState) {
        if (createState is Resource.Success) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nueva Solicitud de Material") })
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
                value = materialName,
                onValueChange = { materialName = it },
                label = { Text("Nombre del Material") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Cantidad") },
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val quantityInt = quantity.toIntOrNull() ?: 0
                    // Por ahora, el ID del material es el mismo que el nombre
                    viewModel.createMaterialRequest(materialName, materialName, quantityInt)
                },
                enabled = createState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (createState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("ENVIAR SOLICITUD")
                }
            }

            if (createState is Resource.Error) {
                Text(
                    text = (createState as Resource.Error<Unit>).message ?: "Error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}