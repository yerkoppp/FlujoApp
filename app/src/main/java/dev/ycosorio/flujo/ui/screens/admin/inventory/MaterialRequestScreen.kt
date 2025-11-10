package dev.ycosorio.flujo.ui.screens.admin.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.ui.components.MaterialRequestItem
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialRequestScreen(
    viewModel: MaterialRequestViewModel = hiltViewModel()
) {
    val requestsState by viewModel.requestsState.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val selectedWarehouse by viewModel.selectedWarehouse.collectAsState()
    val warehouseStock by viewModel.warehouseStock.collectAsState()
    val consolidatedStock by viewModel.consolidatedStock.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedInventoryTab by remember { mutableStateOf(0) }
    var expandedWarehouse by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gestión de Inventario") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow principal
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Solicitudes") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Inventario") }
                )
            }

            // Contenido según pestaña
            when (selectedTab) {
                0 -> {
                    // Pestaña de Solicitudes
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val state = requestsState) {  // <- USAR requestsState
                            is Resource.Idle, is Resource.Loading -> CircularProgressIndicator()
                            is Resource.Success -> {
                                val requests = state.data
                                if (requests.isNullOrEmpty()) {
                                    Text("No hay solicitudes de materiales.")
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            items = requests,
                                            key = { it.id }
                                        ) { request ->
                                            MaterialRequestItem(
                                                role = Role.ADMINISTRADOR,
                                                request = request,
                                                onApprove = {
                                                    viewModel.updateRequestStatus(
                                                        request.id,
                                                        RequestStatus.APROBADO
                                                    )
                                                },
                                                onReject = {
                                                    viewModel.updateRequestStatus(
                                                        request.id,
                                                        RequestStatus.RECHAZADO
                                                    )
                                                },
                                                onDeliver = {
                                                    // Buscar la request completa

                                                    requests.find { it.id == request.id }?.let { req ->
                                                        viewModel.deliverMaterialRequest(req, req.adminNotes)
                                                    }
                                                } as () -> Unit,
                                                onCancel = {}
                                            )
                                        }
                                    }
                                }
                            }
                            is Resource.Error -> Text(state.message ?: "Error al cargar solicitudes")
                        }
                    }
                }
                1 -> {
                    // Pestaña de Inventario
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sub-tabs para inventario
                        TabRow(selectedTabIndex = selectedInventoryTab) {
                            Tab(
                                selected = selectedInventoryTab == 0,
                                onClick = { selectedInventoryTab = 0 },
                                text = { Text("Por Bodega") }
                            )
                            Tab(
                                selected = selectedInventoryTab == 1,
                                onClick = { selectedInventoryTab = 1 },
                                text = { Text("Consolidado") }
                            )
                        }

                        when (selectedInventoryTab) {
                            0 -> {
                                // Inventario por bodega
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    // Selector de bodega
                                    when (val warehousesState = warehouses) {
                                        is Resource.Success -> {
                                            val warehousesList = warehousesState.data ?: emptyList()

                                            ExposedDropdownMenuBox(
                                                expanded = expandedWarehouse,
                                                onExpandedChange = { expandedWarehouse = !expandedWarehouse }
                                            ) {
                                                OutlinedTextField(
                                                    value = warehousesList.find { it.id == selectedWarehouse }?.name ?: "Selecciona una bodega",
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Bodega") },
                                                    trailingIcon = {
                                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWarehouse)
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .menuAnchor()
                                                )

                                                ExposedDropdownMenu(
                                                    expanded = expandedWarehouse,
                                                    onDismissRequest = { expandedWarehouse = false }
                                                ) {
                                                    warehousesList.forEach { warehouse ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Column {
                                                                    Text(warehouse.name)
                                                                    Text(
                                                                        warehouse.type.name,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                viewModel.selectWarehouse(warehouse.id)
                                                                expandedWarehouse = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(16.dp))

                                            // Mostrar stock de la bodega seleccionada
                                            if (selectedWarehouse != null) {
                                                when (val stockState = warehouseStock) {
                                                    is Resource.Loading -> {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator()
                                                        }
                                                    }
                                                    is Resource.Success -> {
                                                        val stock = stockState.data
                                                        if (stock.isNullOrEmpty()) {
                                                            Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text("Esta bodega está vacía.")
                                                            }
                                                        } else {
                                                            LazyColumn(
                                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                items(
                                                                    items = stock,
                                                                    key = { it.id }
                                                                ) { stockItem ->
                                                                    Card(modifier = Modifier.fillMaxWidth()) {
                                                                        Row(
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(16.dp),
                                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Text(
                                                                                text = stockItem.materialName,
                                                                                style = MaterialTheme.typography.titleMedium,
                                                                                modifier = Modifier.weight(1f)
                                                                            )
                                                                            Text(
                                                                                text = "${stockItem.quantity} unidades",
                                                                                style = MaterialTheme.typography.bodyLarge,
                                                                                color = MaterialTheme.colorScheme.primary,
                                                                                fontWeight = FontWeight.Bold
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    is Resource.Error -> {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = stockState.message ?: "Error",
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                    else -> {}
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Selecciona una bodega para ver su inventario")
                                                }
                                            }
                                        }
                                        is Resource.Loading -> {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                        is Resource.Error -> {
                                            Text(
                                                text = warehousesState.message ?: "Error al cargar bodegas",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                            }
                            1 -> {
                                // Inventario consolidado
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (val state = consolidatedStock) {
                                        is Resource.Loading, is Resource.Idle -> CircularProgressIndicator()
                                        is Resource.Success -> {
                                            val consolidated = state.data
                                            if (consolidated.isNullOrEmpty()) {
                                                Text("No hay materiales en el inventario.")
                                            } else {
                                                LazyColumn(
                                                    contentPadding = PaddingValues(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(
                                                        items = consolidated,
                                                        key = { it.materialId }
                                                    ) { consolidatedItem ->
                                                        ConsolidatedStockCard(consolidatedItem = consolidatedItem)
                                                    }
                                                }
                                            }
                                        }
                                        is Resource.Error -> {
                                            Text(
                                                text = state.message ?: "Error al cargar inventario consolidado",
                                                color = MaterialTheme.colorScheme.error
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
    }
}

@Composable
private fun ConsolidatedStockCard(
    consolidatedItem: dev.ycosorio.flujo.domain.model.ConsolidatedStock
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = consolidatedItem.materialName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${consolidatedItem.warehouseBreakdown.size} bodega(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${consolidatedItem.totalQuantity} unidades",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Mostrar desglose si está expandido
            if (expanded && consolidatedItem.warehouseBreakdown.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Desglose por bodega:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                consolidatedItem.warehouseBreakdown.forEach { warehouseStock ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = warehouseStock.warehouseName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${warehouseStock.quantity} unidades",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}