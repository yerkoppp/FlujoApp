package dev.ycosorio.flujo.ui.screens.worker.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.InventoryItem
import dev.ycosorio.flujo.domain.model.Material
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.model.StockItem
import dev.ycosorio.flujo.domain.model.WarehouseType
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.domain.repository.VehicleRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WorkerRequestViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    // --- ESTADO PARA LA LISTA "MIS SOLICITUDES" ---
    private val _myRequestsState = MutableStateFlow<Resource<List<MaterialRequest>>>(Resource.Loading())
    val myRequestsState = _myRequestsState.asStateFlow()

    // --- ESTADO PARA LA ACCIÓN DE CREAR ---
    private val _createRequestState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val createRequestState = _createRequestState.asStateFlow()

    // --- ESTADO PARA LA UI DE BÚSQUEDA ---
    private val _uiState = MutableStateFlow(CreateRequestUiState())
    val uiState: StateFlow<CreateRequestUiState> = _uiState.asStateFlow()

    private val _centralStockFlow = MutableStateFlow<Resource<List<StockItem>>>(Resource.Loading())
    val centralStockFlow: StateFlow<Resource<List<StockItem>>> = _centralStockFlow.asStateFlow()

/*
    // Carga la lista maestra de materiales (tus 'InventoryItem' ahora son 'Material')
    private val _allMaterialsFlow = inventoryRepository.getMaterials()
        .onEach { result ->
            _uiState.update {
                when (result) {
                    is Resource.Loading -> it.copy(isLoadingStock = true)
                    is Resource.Success -> it.copy(
                        allMaterials = result.data ?: emptyList(),
                        isLoadingStock = false
                    )
                    is Resource.Error -> it.copy(
                        error = result.message,
                        isLoadingStock = false
                    )
                    else -> Unit
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    // El filtro ahora busca en la lista maestra de 'Material'
    val filteredMaterials: StateFlow<List<Material>> =
        _uiState.combine(_allMaterialsFlow) { state, materialsResult ->
            val materials = (materialsResult as? Resource.Success)?.data ?: emptyList()

            if (state.searchQuery.isBlank()) {
                emptyList()
            } else {
                materials.filter {
                    it.name.contains(state.searchQuery, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
*/
    private var currentUserId: String? = null

    // --- FLUJO QUE CARGA TODOS LOS MATERIALES ---
    private val materialsFromRepo: Flow<Resource<List<InventoryItem>>> =
        inventoryRepository.getAvailableMaterials()

    // --- FLUJO DE MATERIALES FILTRADOS ---
    // Este es el flujo que te daba error.
    // Ahora usa 'map' para transformar el _uiState (que ya tiene la lista)
    // Esto SÍ devuelve una List<InventoryItem>.
    /*    val filteredMaterials: StateFlow<List<InventoryItem>> =
            _uiState.map { state ->
                if (state.searchQuery.isBlank()) {
                    emptyList()
                } else {
                    state.allMaterials.filter {
                        it.name.contains(state.searchQuery, ignoreCase = true) && it.quantity > 0
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

            // --- FLUJO QUE CARGA TODOS LOS MATERIALES ---
            private val _allMaterialsFlow = inventoryRepository.getAvailableMaterials()
                .onEach { result ->
                    _uiState.update {
                        when (result) {
                            is Resource.Idle, Resource.Loading -> it.copy(isLoadingMaterials = true)
                            is Resource.Success -> it.copy(
                                allMaterials = result.data ?: emptyList(),
                                isLoadingMaterials = false
                            )
                            is Resource.Error -> it.copy(
                                error = result.message,
                                isLoadingMaterials = false
                            )
                            else -> Unit
                        }
                    }
                }
                // stateIn para 'cachear' la lista de materiales.
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())


            // --- FLUJO DE MATERIALES FILTRADOS ---
            val filteredMaterials: StateFlow<List<InventoryItem>> =
                _uiState.combine(_allMaterialsFlow) { state, materialsResult ->

                    // Obtenemos la lista actual de materiales del resultado
                    val materials = (materialsResult as? Resource.Success)?.data ?: emptyList()

                    if (state.searchQuery.isBlank()) {
                        // No mostrar nada si la búsqueda está vacía (o mostrar todo, según prefieras)
                        emptyList()
                    } else {
                        materials.filter {
                            // Filtra por nombre Y asegura que haya cantidad disponible
                            it.name.contains(state.searchQuery, ignoreCase = true) && it.quantity > 0
                        }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        */

    // --- CAMBIO: Flujo que carga el STOCK de la BODEGA CENTRAL ---

    // 1. Encontramos la Bodega Central (Tipo 'FIXED')
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _centralWarehouseFlow = inventoryRepository.getWarehouses()
        .map { result ->
            if (result is Resource.Success) {
                // Asumimos que solo hay UNA bodega FIJA
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
            // 2. Si encontramos la Bodega Central, obtenemos su stock
            when (warehouseResult) {
                is Resource.Success -> {
                    inventoryRepository.getStockForWarehouse(warehouseResult.data!!.id)
                }
                is Resource.Error -> flowOf(Resource.Error(warehouseResult.message!!))
                else -> flowOf(Resource.Loading())
            }
        }
        .onEach { stockResult ->
            // 3. Actualizamos el estado de la UI con el stock de Bodega Central
            _uiState.update {
                when (stockResult) {
                    is Resource.Loading -> it.copy(isLoadingStock = true)
                    is Resource.Success -> it.copy(
                        centralStock = stockResult.data ?: emptyList(),
                        isLoadingStock = false
                    )
                    is Resource.Error -> it.copy(
                        error = stockResult.message,
                        isLoadingStock = false
                    )
                    else -> it
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    // 4. El filtro ahora busca en el 'centralStock' (StockItem de Bodega Central)
    val filteredMaterials: StateFlow<List<StockItem>> =
        _uiState.combine(_centralStockFlow) { state, stockResult ->
            val stock = (stockResult as? Resource.Success)?.data ?: state.centralStock

            if (state.searchQuery.isBlank()) {
                emptyList()
            } else {
                stock.filter {
                    // Filtra por nombre Y asegura que haya cantidad disponible
                    it.materialName.contains(state.searchQuery, ignoreCase = true) && it.quantity > 0
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {

            authRepository.currentUser.collect { authUser -> // <-- CAMBIAR
                currentUserId = authUser?.uid
                //authUser?.let { loadMyRequests(it.uid) }
                if (authUser != null) {
                    // Carga "Mis Solicitudes" (lógica existente)
                    loadMyRequests(authUser.uid)
                    // Busca la bodega del trabajador
                    findAndStoreWorkerWarehouseId(authUser.uid)
                }
            }
        }
        //_allMaterialsFlow.launchIn(viewModelScope)


        /*    // --- Aquí es donde "activamos" el flujo de materiales ---
            // Recolectamos el flujo "frío" y actualizamos el _uiState
            materialsFromRepo.onEach { result ->
                _uiState.update { currentState ->
                    when (result) {
                        is Resource.Loading -> currentState.copy(isLoadingMaterials = true)
                        is Resource.Success -> currentState.copy(
                            isLoadingMaterials = false,
                            allMaterials = result.data ?: emptyList(),
                            error = null
                        )
                        is Resource.Error -> currentState.copy(
                            isLoadingMaterials = false,
                            error = result.message
                        )
                        else -> currentState
                    }
                }
            }.launchIn(viewModelScope) // Esto es correcto aquí
    */
    }

    // --- CAMBIO: Nueva función helper para encontrar la bodega del TRABAJADOR ---
    private fun findAndStoreWorkerWarehouseId(userId: String) {
        viewModelScope.launch {
            try {
                val userResource = userRepository.getUser(userId)
                val user = (userResource as? Resource.Success)?.data
                if (user?.assignedVehicleId == null) {
                    _uiState.update { it.copy(error = "Usuario sin vehículo asignado") }
                    return@launch
                }

                val vehicleResource = vehicleRepository.getVehicle(user.assignedVehicleId)
                val vehicle = (vehicleResource as? Resource.Success)?.data
                if (vehicle?.assignedWarehouseId == null) {
                    _uiState.update { it.copy(error = "Vehículo sin bodega móvil asignada") }
                    return@launch
                }
                // Guardamos la bodega del trabajador en el estado
                _uiState.update { it.copy(workerWarehouseId = vehicle.assignedWarehouseId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error buscando bodega de trabajador") }
            }
        }
    }

    private fun loadMyRequests(userId: String) {
        inventoryRepository.getRequestsForWorker(userId)
            .onEach { result ->
                _myRequestsState.value = result
            }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

  /*  fun createMaterialRequest(item: InventoryItem, quantityToRequest: Int) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            // --- VALIDACIONES ---
            if (quantityToRequest <= 0) {
                _createRequestState.value = Resource.Error("La cantidad debe ser mayor que cero.")
                return@launch
            }

            // Validar la cantidad
            // Volvemos a leer el estado actual de los materiales por seguridad
            val currentItemState = _uiState.value.allMaterials.find { it.id == item.id }

            if (currentItemState == null) {
                _createRequestState.value = Resource.Error("El material ya no existe.")
                return@launch
            }

            if (quantityToRequest > currentItemState.quantity) {
                _createRequestState.value = Resource.Error("No hay suficiente cantidad. Disponible: ${currentItemState.quantity}")
                return@launch
            }

            _createRequestState.value = Resource.Loading()

            val userResource = userRepository.getUser(userId)
            val workerName = if (userResource is Resource.Success) {
                userResource.data?.name ?: "Trabajador"
            } else {
                "Trabajador"
            }

            val newRequest = MaterialRequest(
                id = UUID.randomUUID().toString(),
                workerId = userId,
                workerName = workerName,
                materialId = item.id, // <-- CORREGIDO
                materialName = item.name, // <-- CORREGIDO
                quantity = quantityToRequest, // <-- CORREGIDO
                status = RequestStatus.PENDIENTE,
                requestDate = Date()
            )
            _createRequestState.value = inventoryRepository.createMaterialRequest(newRequest)

            // Si la creación es exitosa, limpiar el query de búsqueda
            if (_createRequestState.value is Resource.Success) {
                onSearchQueryChanged("")
            }
        }
    }*/

  /*  /**
     * Crea una solicitud de material. La solicitud se vincula a la bodega
     * (Warehouse) que el trabajador tiene asignada a través de su vehículo.
     */
    fun createMaterialRequest(material: Material, quantity: Int) {
        val userId = currentUserId
        if (userId == null) {
            _createRequestState.value = Resource.Error("No se pudo obtener el usuario actual.")
            return
        }

        viewModelScope.launch {
            // --- VALIDACIÓN (de tu lógica original) ---
            if (quantity <= 0) {
                _createRequestState.value = Resource.Error("La cantidad debe ser mayor que cero.")
                return@launch
            }

            _createRequestState.value = Resource.Loading()

            try {
                // 1. Obtener el nombre del trabajador (lógica original)
                val userResource = userRepository.getUser(userId)
                val workerName = (userResource as? Resource.Success)?.data?.name ?: "Trabajador"

                // 2. Encontrar la BODEGA (Warehouse) del trabajador (NUEVA LÓGICA)
                val user = (userResource as? Resource.Success)?.data
                if (user?.assignedVehicleId == null) {
                    throw Exception("El usuario no tiene un vehículo asignado.")
                }

                val vehicleResource = vehicleRepository.getVehicle(user.assignedVehicleId)
                val vehicle = (vehicleResource as? Resource.Success)?.data
                if (vehicle?.assignedWarehouseId == null) {
                    throw Exception("El vehículo no tiene una bodega móvil asignada.")
                }
                val warehouseId = vehicle.assignedWarehouseId

                // 3. Crear la solicitud con el modelo actualizado
                val newRequest = MaterialRequest(
                    id = UUID.randomUUID().toString(),
                    workerId = userId,
                    workerName = workerName,
                    warehouseId = warehouseId, // ID de la bodega del trabajador
                    materialId = material.id,  // ID del material maestro
                    materialName = material.name, // Nombre del material maestro
                    quantity = quantity,
                    status = RequestStatus.PENDIENTE,
                    requestDate = Date()
                )

                // 4. Enviar al repositorio
                _createRequestState.value = inventoryRepository.createMaterialRequest(newRequest)

                // Limpiar búsqueda si fue exitoso
                if (_createRequestState.value is Resource.Success) {
                    onSearchQueryChanged("")
                }

            } catch (e: Exception) {
                _createRequestState.value =
                    Resource.Error(e.message ?: "Error al procesar la solicitud")
            }
        }
    }*/

    /**
     * Crea una solicitud de material desde Bodega Central para la bodega del trabajador.
     * @param item El StockItem de la Bodega Central (para validación de cantidad).
     * @param quantity La cantidad solicitada.
     */
    fun createMaterialRequest(item: StockItem, quantity: Int) {
        val userId = currentUserId
        val workerWarehouseId = _uiState.value.workerWarehouseId // La bodega de DESTINO

        if (userId == null || workerWarehouseId == null) {
            _createRequestState.value = Resource.Error("No se pudo obtener el usuario o la bodega de destino.")
            return
        }

        viewModelScope.launch {
            // --- VALIDACIÓN (Corregida) ---
            if (quantity <= 0) {
                _createRequestState.value = Resource.Error("La cantidad debe ser mayor que cero.")
                return@launch
            }

            // Leemos el estado actual del stock DE BODEGA CENTRAL
            val currentItemState = _uiState.value.centralStock.find { it.id == item.id }
            if (currentItemState == null) {
                _createRequestState.value = Resource.Error("El material ya no existe en Bodega Central.")
                return@launch
            }

            // Esta es la validación clave:
            if (quantity > currentItemState.quantity) {
                _createRequestState.value = Resource.Error("No puedes solicitar más de lo disponible en Bodega Central. Disponible: ${currentItemState.quantity}")
                return@launch
            }
            // --- FIN DE VALIDACIÓN ---

            _createRequestState.value = Resource.Loading()

            try {
                // 1. Obtener el nombre del trabajador (lógica existente)
                val userResource = userRepository.getUser(userId)
                val workerName = (userResource as? Resource.Success)?.data?.name ?: "Trabajador"

                // 2. Crear la solicitud (basado en el modelo de Paso 6)
                val newRequest = MaterialRequest(
                    id = UUID.randomUUID().toString(),
                    workerId = userId,
                    workerName = workerName,
                    warehouseId = workerWarehouseId, // ID de la bodega del trabajador (DESTINO)
                    materialId = item.materialId,
                    materialName = item.materialName,
                    quantity = quantity,
                    status = RequestStatus.PENDIENTE,
                    requestDate = Date()
                )

                // 3. Enviar al repositorio
                _createRequestState.value = inventoryRepository.createMaterialRequest(newRequest)

                if (_createRequestState.value is Resource.Success) {
                    onSearchQueryChanged("")
                }

            } catch (e: Exception) {
                _createRequestState.value =
                    Resource.Error(e.message ?: "Error al procesar la solicitud")
            }
        }
    }

    /**
     * Resetea el estado de creación a Idle. Útil después de mostrar un error.
     */
    fun resetCreateState() {
        _createRequestState.value = Resource.Idle()
    }
}

// --- ESTADO PARA LA PANTALLA DE CREACIÓN ---
data class CreateRequestUiState(
    val allMaterials: List<Material> = emptyList(),
    val centralStock: List<StockItem> = emptyList(),
    val searchQuery: String = "",
    val isLoadingStock: Boolean = false,
    val error: String? = null,
    val workerWarehouseId: String? = null, // La bodega del trabajador (destino)
    val centralWarehouseId: String? = null
)