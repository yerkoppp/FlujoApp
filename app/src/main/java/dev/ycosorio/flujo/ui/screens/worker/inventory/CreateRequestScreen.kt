package dev.ycosorio.flujo.ui.screens.worker.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ycosorio.flujo.domain.model.Material
import dev.ycosorio.flujo.domain.model.StockItem
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    // Inyectamos el ViewModel que es compartido por WorkerRequestScreen
    viewModel: CreateRequestViewModel = hiltViewModel(),
    onSuccess: () -> Unit

) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredMaterials by viewModel.filteredMaterials.collectAsStateWithLifecycle()
    val createRequestState by viewModel.createRequestState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Estado para controlar qué item estamos solicitando (para el diálogo)
    var itemToRequest by remember { mutableStateOf<StockItem?>(null) }

    // Efecto para mostrar Snackbars de éxito o error al crear
    LaunchedEffect(createRequestState) {
        when (val state = createRequestState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Solicitud creada con éxito")
                viewModel.resetCreateState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(state.message ?: "Error desconocido")
                viewModel.resetCreateState()
            }
            else -> Unit
        }
    }

    // Usamos un Scaffold simple solo para el SnackbarHost
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Nueva Solicitud de Material",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- CAMPO DE BÚSQUEDA ---
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar material...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    isError = uiState.error != null
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- LISTA DE RESULTADOS ---
                if (uiState.isLoadingStock) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (filteredMaterials.isEmpty() && uiState.searchQuery.isNotBlank()) {
                    Text(
                        text = "No se encontraron materiales con ese nombre o no hay cantidad disponible.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredMaterials, key = { it.id }) { item ->
                            MaterialSearchResultItem(
                                item = item,
                                onClick = {
                                    // Abre el diálogo para pedir cantidad
                                    itemToRequest = item
                                }
                            )
                        }
                    }
                }
            }

            // Muestra un indicador de carga si estamos creando la solicitud
            if (createRequestState is Resource.Loading) {
                CircularProgressIndicator()
            }
        }
    }


    // --- DIÁLOGO PARA PEDIR CANTIDAD ---
    itemToRequest?.let { item ->
        RequestQuantityDialog(
            item = item,
            onDismiss = { itemToRequest = null },
            onConfirm = { quantity ->
                viewModel.createMaterialRequest(item, quantity)
                itemToRequest = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialSearchResultItem(
    item: StockItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        ListItem(
            headlineContent = { Text(item.materialName) },
            supportingContent = { Text("Cantidad disponible: " + item.quantity) }
        )
    }
}

@Composable
private fun RequestQuantityDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int) -> Unit
) {
    var quantity by rememberSaveable { mutableStateOf("") }
    var quantityError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar: ${item.materialName}") },
        text = {
            Column {
                //Text("Cantidad disponible: ${item.quantity}")
                //Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it.filter { char -> char.isDigit() }
                        quantityError = null
                    },
                    label = { Text("Cantidad a solicitar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantityError != null,
                    supportingText = { if (quantityError != null) Text(quantityError!!) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantityInt = quantity.toIntOrNull()
                    // Validación
                    if (quantityInt == null || quantityInt <= 0) {
                        quantityError = "Cantidad inválida"
                    } else if (quantityInt > item.quantity) {
                        quantityError = "No puedes pedir más de lo disponible (${item.quantity})"
                    } else {
                        // ¡Éxito!
                        onConfirm(quantityInt)
                    }
                }
            ) {
                Text("Solicitar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}