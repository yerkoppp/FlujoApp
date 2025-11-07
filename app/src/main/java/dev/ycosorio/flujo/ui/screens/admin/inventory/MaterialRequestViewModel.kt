package dev.ycosorio.flujo.ui.screens.admin.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaterialRequestViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _requestsState = MutableStateFlow<Resource<List<MaterialRequest>>>(Resource.Loading())
    val requestsState = _requestsState.asStateFlow()

    // Estados para los filtros y el orden
    private val _statusFilter = MutableStateFlow<RequestStatus?>(null) // null significa "todos"
    val statusFilter = _statusFilter.asStateFlow()

    private val _orderBy = MutableStateFlow("requestDate")
    val orderBy = _orderBy.asStateFlow()

    init {
        loadRequests()
    }

    private fun loadRequests() {
        inventoryRepository.getMaterialRequests(
            orderBy = _orderBy.value,
            statusFilter = _statusFilter.value
            // La dirección por ahora es fija (DESCENDING), pero podría ser otro StateFlow
        ).onEach { result ->
            _requestsState.value = result
        }.launchIn(viewModelScope)
    }

    /**
     * Actualiza el filtro de estado y vuelve a cargar la lista de solicitudes.
     */
    fun onStatusFilterChanged(newStatus: RequestStatus?) {
        _statusFilter.value = newStatus
        loadRequests()
    }

    /**
     * Actualiza el campo de ordenamiento y vuelve a cargar la lista.
     */
    fun onOrderByChanged(newOrderBy: String) {
        _orderBy.value = newOrderBy
        loadRequests()
    }

    /**
     * Actualiza el estado de una solicitud específica.
     * @param requestId El ID de la solicitud a modificar.
     * @param newStatus El nuevo estado (APROBADO o RECHAZADO).
     */
    fun updateRequestStatus(requestId: String, newStatus: RequestStatus, adminNotes: String? = null) {
        viewModelScope.launch {
            // Podríamos añadir un StateFlow para el estado de la actualización si queremos mostrar un loader
            inventoryRepository.updateRequestStatus(requestId, newStatus, adminNotes)

        }
    }

}