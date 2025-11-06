package dev.ycosorio.flujo.ui.screens.worker.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.InventoryItem
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.utils.SimulationAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WorkerRequestViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- ESTADO PARA LA LISTA "MIS SOLICITUDES" ---
    private val _myRequestsState = MutableStateFlow<Resource<List<MaterialRequest>>>(Resource.Loading())
    val myRequestsState = _myRequestsState.asStateFlow()

    // --- ESTADO PARA LA ACCIÓN DE CREAR ---
    private val _createRequestState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val createRequestState = _createRequestState.asStateFlow()

    // --- NUEVO: ESTADO PARA LA UI DE BÚSQUEDA ---
    private val _uiState = MutableStateFlow(CreateRequestUiState())
    val uiState: StateFlow<CreateRequestUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    // --- FLUJO QUE CARGA TODOS LOS MATERIALES ---
    private val materialsFromRepo: Flow<Resource<List<InventoryItem>>> =
        inventoryRepository.getAvailableMaterials()

    // --- [CAMBIO] FLUJO DE MATERIALES FILTRADOS ---
    // Este es el flujo que te daba error.
    // Ahora usa 'map' para transformar el _uiState (que ya tiene la lista)
    // Esto SÍ devuelve una List<InventoryItem>.
    val filteredMaterials: StateFlow<List<InventoryItem>> =
        _uiState.map { state ->
            if (state.searchQuery.isBlank()) {
                emptyList()
            } else {
                state.allMaterials.filter {
                    it.name.contains(state.searchQuery, ignoreCase = true) && it.quantity > 0
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    /*
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
    init {
        viewModelScope.launch {

            authRepository.currentUser.collect { authUser -> // <-- CAMBIAR
                currentUserId = authUser?.uid
                authUser?.let { loadMyRequests(it.uid) }
            }
        }

        // --- Aquí es donde "activamos" el flujo de materiales ---
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

    fun createMaterialRequest(item: InventoryItem, quantityToRequest: Int) {
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
    val allMaterials: List<InventoryItem> = emptyList(),
    val searchQuery: String = "",
    val isLoadingMaterials: Boolean = false,
    val error: String? = null
)