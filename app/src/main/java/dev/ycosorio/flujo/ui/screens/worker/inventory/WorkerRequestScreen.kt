package dev.ycosorio.flujo.ui.screens.worker.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.google.firebase.auth.FirebaseAuth
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.ui.components.MaterialRequestItem
import dev.ycosorio.flujo.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerRequestScreen(
    viewModel: WorkerRequestViewModel,
    navController: NavController,
    onAddRequestClicked: () -> Unit
) {

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val myRequestsState by viewModel.myRequestsState.collectAsState()
    val myWarehouseStock by viewModel.myWarehouseStock.collectAsState()
    val myWarehouse by viewModel.myWarehouse.collectAsState()

    val requestsPaged = viewModel.getWorkerRequestsPaged(currentUserId).collectAsLazyPagingItems()

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Inventario") })
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = onAddRequestClicked) {
                    Icon(Icons.Default.Add, contentDescription = "Crear nueva solicitud")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Mis Solicitudes") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Mi Inventario") }
                )
            }

            // Contenido según pestaña
            when (selectedTab) {
                0 -> {
                    // ✅ PESTAÑA DE SOLICITUDES - CON PAGINACIÓN
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
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
                                    Text("Error al cargar solicitudes")
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
                                    Text("No has realizado ninguna solicitud.")
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
                                                role = Role.TRABAJADOR,
                                                request = request,
                                                onApprove = {},
                                                onReject = {},
                                                onDeliver = {},
                                                onCancel = {
                                                    viewModel.cancelRequest(request.id)
                                                }
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
                1 -> {
                    // Pestaña de Mi Inventario
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Mostrar nombre de bodega
                        myWarehouse?.let { warehouse ->
                            Text(
                                text = "Bodega: ${warehouse.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Tipo: ${warehouse.type.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } ?: run {
                            Text(
                                text = "No tienes una bodega asignada",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Mostrar stock
                        when (val state = myWarehouseStock) {
                            is Resource.Idle, is Resource.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is Resource.Success -> {
                                if (state.data.isNullOrEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Tu bodega está vacía.")
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            items = state.data,
                                            key = { it.id }
                                        ) { stockItem ->
                                            StockItemCard(stockItem = stockItem)
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
                                        text = state.message ?: "Error al cargar inventario",
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

@Composable
private fun StockItemCard(stockItem: dev.ycosorio.flujo.domain.model.StockItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stockItem.materialName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${stockItem.quantity} unidades",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}