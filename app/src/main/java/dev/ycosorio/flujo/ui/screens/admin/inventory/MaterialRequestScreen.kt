package dev.ycosorio.flujo.ui.screens.admin.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.ui.components.MaterialRequestItem
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.domain.model.ConsolidatedStock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialRequestScreen(
    navController: NavController,
    viewModel: MaterialRequestViewModel = hiltViewModel()
) {
    val requestsState by viewModel.requestsState.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val selectedWarehouse by viewModel.selectedWarehouse.collectAsState()
    val warehouseStock by viewModel.warehouseStock.collectAsState()
    val consolidatedStock by viewModel.consolidatedStock.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    val requestsPaged = viewModel.getMaterialRequestsPaged(statusFilter).collectAsLazyPagingItems()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedInventoryTab by remember { mutableStateOf(0) }
    var expandedWarehouse by remember { mutableStateOf(false) }

    var expandedFilter by remember { mutableStateOf(false) }
    val filterOptions = listOf(
        null to "Todos",
        RequestStatus.PENDIENTE to "Pendientes",
        RequestStatus.APROBADO to "Aprobadas",
        RequestStatus.RECHAZADO to "Rechazadas",
        RequestStatus.ENTREGADO to "Entregadas",
        RequestStatus.CANCELADO to "Canceladas"
    )
    val selectedLabel = filterOptions.find { it.first == statusFilter }?.second ?: "Todos"


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
            PrimaryTabRow(selectedTabIndex = selectedTab) {
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
                    // ✅ PESTAÑA DE SOLICITUDES - CON FILTRO Y PAGINACIÓN
                    Column(modifier = Modifier.fillMaxSize()) {

                        // ✅ RESTAURADO: Filtro dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedFilter,
                            onExpandedChange = { expandedFilter = !expandedFilter },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = "Filtrar: $selectedLabel",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Estado") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFilter)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expandedFilter,
                                onDismissRequest = { expandedFilter = false }
                            ) {
                                filterOptions.forEach { (status, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.onStatusFilterChanged(status)
                                            viewModel.refreshWithFilter(status)
                                            expandedFilter = false
                                        },
                                        leadingIcon = if (statusFilter == status) {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                        }

                        // ✅ NUEVO: Lista paginada
                        Box(modifier = Modifier.fillMaxSize()) {
                            when {
                                requestsPaged.loadState.refresh is LoadState.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                                requestsPaged.loadState.refresh is LoadState.Error -> {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Error al cargar solicitudes",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { requestsPaged.retry() }) {
                                            Text("Reintentar")
                                        }
                                    }
                                }
                                requestsPaged.itemCount == 0 -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No hay solicitudes de materiales.")
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            count = requestsPaged.itemCount,
                                            key = requestsPaged.itemKey { it.id }
                                        ) { index ->
                                            val request = requestsPaged[index]
                                            if (request != null) {
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
                                                        viewModel.deliverMaterialRequest(request, request.adminNotes)
                                                    } as () -> Unit,
                                                    onCancel = {}
                                                )
                                            }
                                        }

                                        // Loading al cargar más
                                        if (requestsPaged.loadState.append is LoadState.Loading) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator()
                                                }
                                            }
                                        }

                                        // Error al cargar más
                                        if (requestsPaged.loadState.append is LoadState.Error) {
                                            item {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text("Error al cargar más solicitudes")
                                                    Button(onClick = { requestsPaged.retry() }) {
                                                        Text("Reintentar")
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
                1 -> {
                    // Pestaña de Inventario
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sub-tabs para inventario
                        SecondaryTabRow(selectedTabIndex = selectedInventoryTab) {
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
    consolidatedItem: ConsolidatedStock
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${consolidatedItem.warehouseBreakdown.size} bodega(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${consolidatedItem.totalQuantity} unidades",
                    style = MaterialTheme.typography.titleLarge,
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