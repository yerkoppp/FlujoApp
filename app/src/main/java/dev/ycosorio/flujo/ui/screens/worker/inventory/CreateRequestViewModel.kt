package dev.ycosorio.flujo.ui.screens.worker.inventory

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.WarehouseType
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.domain.repository.VehicleRepository
import dev.ycosorio.flujo.domain.model.RequestItem
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.model.StockItem
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn

@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val userRepository: UserRepository,
    authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _createRequestState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val createRequestState = _createRequestState.asStateFlow()

    private val _uiState = MutableStateFlow(CreateRequestUiState())
    val uiState: StateFlow<CreateRequestUiState> = _uiState.asStateFlow()

    private val _selectedMaterials = MutableStateFlow<List<SelectedMaterial>>(emptyList())
    val selectedMaterials = _selectedMaterials.asStateFlow()

    private var currentUserId: String? = null

    // --- FLUJO: Encuentra Bodega Central y carga su stock ---
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _centralWarehouseFlow = inventoryRepository.getWarehouses()
        .map { result ->
            if (result is Resource.Success) {
                val centralWarehouse = result.data?.find { it.type == WarehouseType.FIXED }
                if (centralWarehouse != null) {
                    _uiState.update { it.copy(centralWarehouseId = centralWarehouse.id) }
                    Resource.Success(centralWarehouse)
                } else {
                    Resource.Error("No se encontró Bodega Central (FIXED)")
                }
            } else if (result is Resource.Error) {
                Resource.Error(result.message ?: "Error cargando bodegas")
            } else {
                Resource.Loading()
            }
        }
        .flatMapLatest { warehouseResult ->
            when (warehouseResult) {
                is Resource.Success -> {
                    inventoryRepository.getStockForWarehouse(warehouseResult.data!!.id)
                }

                is Resource.Error -> flowOf(Resource.Error(warehouseResult.message!!))
                else -> flowOf(Resource.Loading())
            }
        }
        .onEach { stockResult ->
            _uiState.update {
                when (stockResult) {
                    is Resource.Loading -> it.copy(isLoadingStock = true)
                    is Resource.Success -> it.copy(
                        centralStock = stockResult.data ?: emptyList(),
                        isLoadingStock = false,
                        error = null
                    )

                    is Resource.Error -> it.copy(
                        error = stockResult.message,
                        isLoadingStock = false
                    )

                    else -> it
                }
            }
        }

    // --- FLUJO: Filtrar materiales según búsqueda ---
    val filteredMaterials: StateFlow<List<StockItem>> =
        _uiState.map { state ->
            if (state.searchQuery.isBlank()) {
                emptyList()
            } else {
                state.centralStock.filter {
                    it.materialName.contains(
                        state.searchQuery,
                        ignoreCase = true
                    ) && it.quantity > 0
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        authRepository.currentUser.onEach { authUser ->
            authUser?.let { user ->
                currentUserId = user.uid

                // Cargar el vehículo asignado para obtener su bodega móvil
                vehicleRepository.getVehicles().onEach { vehiclesResult ->
                    if (vehiclesResult is Resource.Success) {
                        val myVehicle = vehiclesResult.data?.find { it.userIds.contains(user.uid) }
                        myVehicle?.assignedWarehouseId?.let { warehouseId ->
                            _uiState.update { it.copy(workerWarehouseId = warehouseId) }
                        }
                    }
                }.launchIn(viewModelScope)
            }
        }.launchIn(viewModelScope)

        // Lanzar el flow de bodega central
        _centralWarehouseFlow.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Agrega un material al carrito temporal
     */
    fun addMaterialToCart(item: StockItem, quantity: Int) {
        val material = SelectedMaterial(
            stockItem = item,
            requestedQuantity = quantity
        )
        _selectedMaterials.update { current ->
            current + material
        }
        // Limpiar búsqueda
        onSearchQueryChanged("")
    }

    /**
     * Remueve un material del carrito
     */
    fun removeMaterialFromCart(index: Int) {
        _selectedMaterials.update { current ->
            current.filterIndexed { i, _ -> i != index }
        }
    }

    /**
     * Crea múltiples solicitudes para todos los materiales en el carrito
     */
    fun submitMultipleMaterialRequests() {
        val userId = currentUserId
        val workerWarehouseId = _uiState.value.workerWarehouseId

        if (userId == null || workerWarehouseId == null) {
            _createRequestState.value = Resource.Error("No se pudo obtener el usuario o la bodega de destino.")
            return
        }

        if (_selectedMaterials.value.isEmpty()) {
            _createRequestState.value = Resource.Error("Agrega al menos un material a la solicitud.")
            return
        }

        viewModelScope.launch {
            _createRequestState.value = Resource.Loading()

            try {
                // Obtener nombre del trabajador
                val userResource = userRepository.getUser(userId)
                val workerName = (userResource as? Resource.Success)?.data?.name ?: "Trabajador"

                // Crear lista de items
                val requestItems = _selectedMaterials.value.map { selected ->
                    RequestItem(
                        materialId = selected.stockItem.materialId,
                        materialName = selected.stockItem.materialName,
                        quantity = selected.requestedQuantity
                    )
                }
                // Crear UNA sola solicitud con múltiples items
                val newRequest = MaterialRequest(
                    id = UUID.randomUUID().toString(),
                    workerId = userId,
                    workerName = workerName,
                    warehouseId = workerWarehouseId,
                    items = requestItems,
                    status = RequestStatus.PENDIENTE,
                    requestDate = Date(),
                    approvalDate = null,
                    rejectionDate = null,
                    deliveryDate = null,
                    adminNotes = null
                )
                _createRequestState.value = inventoryRepository.createMaterialRequest(newRequest)

                if (_createRequestState.value is Resource.Success) {
                    _selectedMaterials.value = emptyList() // Limpiar carrito
                }

            } catch (e: Exception) {
                _createRequestState.value = Resource.Error(e.message ?: "Error al procesar las solicitudes")
            }
        }
    }

    /**
     * Crea una solicitud de material desde Bodega Central para la bodega del trabajador.
     */
  /*  fun createMaterialRequest(item: StockItem, quantity: Int) {
        val userId = currentUserId
        val workerWarehouseId = _uiState.value.workerWarehouseId

        if (userId == null || workerWarehouseId == null) {
            _createRequestState.value = Resource.Error("No se pudo obtener el usuario o la bodega de destino.")
            return
        }

        viewModelScope.launch {
            // --- VALIDACIONES ---
            if (quantity <= 0) {
                _createRequestState.value = Resource.Error("La cantidad debe ser mayor que cero.")
                return@launch
            }

            // Validar contra el stock ACTUAL de Bodega Central
            val currentItemState = _uiState.value.centralStock.find { it.id == item.id }
            if (currentItemState == null) {
                _createRequestState.value = Resource.Error("El material ya no existe en Bodega Central.")
                return@launch
            }

            if (quantity > currentItemState.quantity) {
                _createRequestState.value = Resource.Error(
                    "No puedes solicitar más de lo disponible en Bodega Central. Disponible: ${currentItemState.quantity}"
                )
                return@launch
            }

            _createRequestState.value = Resource.Loading()

            try {
                // Obtener nombre del trabajador
                val userResource = userRepository.getUser(userId)
                val workerName = (userResource as? Resource.Success)?.data?.name ?: "Trabajador"

                // Crear la solicitud
                val newRequest = MaterialRequest(
                    id = UUID.randomUUID().toString(),
                    workerId = userId,
                    workerName = workerName,
                    warehouseId = workerWarehouseId,
                    materialId = item.materialId,
                    materialName = item.materialName,
                    quantity = quantity,
                    status = RequestStatus.PENDIENTE,
                    requestDate = Date(),
                    approvalDate = null,
                    rejectionDate = null,
                    deliveryDate = null,
                    adminNotes = null
                )

                _createRequestState.value = inventoryRepository.createMaterialRequest(newRequest)

                if (_createRequestState.value is Resource.Success) {
                    onSearchQueryChanged("")
                }

            } catch (e: Exception) {
                _createRequestState.value =
                    Resource.Error(e.message ?: "Error al procesar la solicitud")
            }
        }
    }*/

    fun resetCreateState() {
        _createRequestState.value = Resource.Idle()
    }
}

// --- ESTADO PARA LA PANTALLA DE CREACIÓN ---
data class CreateRequestUiState(
    val centralStock: List<StockItem> = emptyList(),
    val searchQuery: String = "",
    val isLoadingStock: Boolean = false,
    val error: String? = null,
    val workerWarehouseId: String? = null,
    val centralWarehouseId: String? = null
)

// --- MODELO PARA MATERIALES SELECCIONADOS ---
data class SelectedMaterial(
    val stockItem: StockItem,
    val requestedQuantity: Int
)