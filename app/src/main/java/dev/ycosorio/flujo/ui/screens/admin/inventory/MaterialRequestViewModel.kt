package dev.ycosorio.flujo.ui.screens.admin.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.model.ConsolidatedStock
import dev.ycosorio.flujo.domain.model.StockItem
import dev.ycosorio.flujo.domain.model.Warehouse
import dev.ycosorio.flujo.domain.model.WarehouseType
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MaterialRequestViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _selectedWarehouse = MutableStateFlow<String?>(null)
    val selectedWarehouse = _selectedWarehouse.asStateFlow()

    private val _warehouseStock = MutableStateFlow<Resource<List<StockItem>>>(Resource.Idle())
    val warehouseStock = _warehouseStock.asStateFlow()

    private val _consolidatedStock = MutableStateFlow<Resource<List<ConsolidatedStock>>>(Resource.Idle())
    val consolidatedStock = _consolidatedStock.asStateFlow()

    private val _warehouses = MutableStateFlow<Resource<List<Warehouse>>>(Resource.Idle())
    val warehouses = _warehouses.asStateFlow()

    // Estado del filtro
    private val _statusFilter = MutableStateFlow<RequestStatus?>(null)
    val statusFilter: StateFlow<RequestStatus?> = _statusFilter.asStateFlow()

    // StateFlow reactivo de solicitudes (CON SnapshotListener automático)
    @OptIn(ExperimentalCoroutinesApi::class)
    val requestsState: StateFlow<Resource<List<MaterialRequest>>> =
        _statusFilter.flatMapLatest { status ->
            android.util.Log.d("MaterialRequestVM", "🔄 Filtro cambiado a: $status")
            inventoryRepository.getMaterialRequests(
                orderBy = "requestDate",
                direction = com.google.firebase.firestore.Query.Direction.DESCENDING,
                statusFilter = status
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )

    init {
        // Cargar bodegas
        inventoryRepository.getWarehouses().onEach {
            _warehouses.value = it
        }.launchIn(viewModelScope)

        // Cargar inventario consolidado
        inventoryRepository.getConsolidatedInventory().onEach {
            _consolidatedStock.value = it
        }.launchIn(viewModelScope)
    }

    fun onStatusFilterChanged(newStatus: RequestStatus?) {
        _statusFilter.value = newStatus
    }

    fun updateRequestStatus(requestId: String, newStatus: RequestStatus, adminNotes: String? = null) {
        viewModelScope.launch {
            android.util.Log.d("MaterialRequestVM", "🔄 Actualizando estado: $requestId -> $newStatus")
            inventoryRepository.updateRequestStatus(requestId, newStatus, adminNotes)
            // El SnapshotListener actualizará automáticamente
        }
    }

    fun deliverMaterialRequest(request: MaterialRequest, adminNotes: String? = null) {
        viewModelScope.launch {
            if (request.status != RequestStatus.APROBADO) {
                Timber.tag("MaterialRequestVM").e("❌ Estado incorrecto: ${request.status}")
                return@launch
            }

            Timber.tag("MaterialRequestVM").d("📦 Iniciando entrega de: ${request.id}")

            val warehousesState = _warehouses.value

            if (warehousesState is Resource.Success) {
                val centralWarehouse = warehousesState.data?.find { it.type == WarehouseType.FIXED }

                if (centralWarehouse == null) {
                    Timber.tag("MaterialRequestVM").e("❌ No hay bodega central")
                    return@launch
                }

                Timber.tag("MaterialRequestVM").d("✅ Bodega central: ${centralWarehouse.name}")

                val result = inventoryRepository.deliverMaterialRequest(
                    requestId = request.id,
                    centralWarehouseId = centralWarehouse.id,
                    adminNotes = adminNotes
                )

                when (result) {
                    is Resource.Success -> {
                        Timber.tag("MaterialRequestVM").d("✅ Entregado exitosamente")
                        // El SnapshotListener actualizará automáticamente
                    }
                    is Resource.Error -> {
                        Timber.tag("MaterialRequestVM").e("❌ Error: ${result.message}")
                    }
                    else -> {}
                }
            } else {
                Timber.tag("MaterialRequestVM").e("❌ Bodegas no disponibles")
            }
        }
    }

    fun selectWarehouse(warehouseId: String?) {
        _selectedWarehouse.value = warehouseId
        warehouseId?.let {
            inventoryRepository.getStockForWarehouse(it).onEach { result ->
                _warehouseStock.value = result
            }.launchIn(viewModelScope)
        }
    }
}