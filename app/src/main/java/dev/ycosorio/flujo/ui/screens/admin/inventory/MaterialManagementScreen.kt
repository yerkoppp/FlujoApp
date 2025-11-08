package dev.ycosorio.flujo.ui.screens.admin.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.ycosorio.flujo.domain.model.InventoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialManagementScreen(
    navController: NavController,
    viewModel: MaterialManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados para diálogos
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showAddStockDialog by remember { mutableStateOf<InventoryItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is MaterialManagementViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Materiales") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Crear Material")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            MaterialListContent(
                state = uiState,
                onAddStockClick = { item -> showAddStockDialog = item }
            )

            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
            }
        }
    }

    // --- DIÁLOGOS ---

    if (showCreateDialog) {
        CreateMaterialDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, stock ->
                viewModel.createMaterial(name, stock)
                showCreateDialog = false
            }
        )
    }

    showAddStockDialog?.let { item ->
        AddQuantityDialog(
            item = item,
            onDismiss = { showAddStockDialog = null },
            onConfirm = { amount ->
                viewModel.addStock(item.id, amount)
                showAddStockDialog = null
            }
        )
    }
}

@Composable
private fun MaterialListContent(
    state: MaterialManagementUiState,
    onAddStockClick: (InventoryItem) -> Unit
) {
    if (state.materials.isEmpty() && !state.isLoading) {
        Text(
            text = "No hay materiales creados.\nPresiona (+) para añadir.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.materials, key = { it.id }) { item ->
                MaterialItemCard(
                    item = item,
                    onAddStockClick = { onAddStockClick(item) }
                )
            }
        }
    }
}

@Composable
private fun MaterialItemCard(
    item: InventoryItem,
    onAddStockClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stock: ${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedButton(onClick = onAddStockClick) {
                Text("Añadir Stock")
            }
        }
    }
}

@Composable
private fun CreateMaterialDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, stock: Int) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("0") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var stockError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nuevo Material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Nombre del material") },
                    isError = nameError != null,
                    supportingText = { if (nameError != null) Text(nameError!!) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = {
                        stock = it.filter { char -> char.isDigit() }
                        stockError = null
                    },
                    label = { Text("Stock Inicial") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = stockError != null,
                    supportingText = { if (stockError != null) Text(stockError!!) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stockInt = stock.toIntOrNull()
                    var hasError = false
                    if (name.isBlank()) {
                        nameError = "El nombre es obligatorio"
                        hasError = true
                    }
                    if (stockInt == null || stockInt < 0) {
                        stockError = "Stock inválido"
                        hasError = true
                    }
                    if (!hasError) {
                        onCreate(name, stockInt!!)
                    }
                }
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun AddQuantityDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (amount: Int) -> Unit
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var amountError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir cantidad a ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stock actual: ${item.quantity}")
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { char -> char.isDigit() }
                        amountError = null
                    },
                    label = { Text("Cantidad a añadir") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError != null,
                    supportingText = { if (amountError != null) Text(amountError!!) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountInt = amount.toIntOrNull()
                    if (amountInt == null || amountInt <= 0) {
                        amountError = "Debe ser un número positivo"
                    } else {
                        onConfirm(amountInt)
                    }
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}