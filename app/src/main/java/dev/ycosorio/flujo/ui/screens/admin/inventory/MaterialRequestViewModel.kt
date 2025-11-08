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
import dev.ycosorio.flujo.domain.model.WarehouseType
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

    /**
     * Actualiza el estado de una solicitud a ENTREGADO y transfiere el stock.
     * @param request La solicitud completa (necesitamos varios campos).
     */
    fun markAsDelivered(request: MaterialRequest) {
        viewModelScope.launch {
            // 1. Cambiar estado a ENTREGADO
            val result = inventoryRepository.updateRequestStatus(
                requestId = request.id,
                status = RequestStatus.ENTREGADO,
                adminNotes = null // O puedes pedir notas al admin
            )

            if (result is Resource.Success) {
                // 2. Transferir stock de Bodega Central → Bodega Móvil
                // Necesitamos obtener el objeto Material completo
                // (esto requiere una consulta adicional o pasarlo desde la UI)

                // TODO: Implementar transferencia de stock
                // inventoryRepository.transferStock(
                //     fromWarehouseId = BODEGA_CENTRAL_ID,
                //     toWarehouseId = request.warehouseId,
                //     material = ...,
                //     quantityToTransfer = request.quantity
                // )
            }
        }
    }

    /**
     * Marca una solicitud como ENTREGADA y transfiere el stock automáticamente.
     * Esta operación valida que la solicitud esté APROBADA y que haya stock disponible.
     */
    fun deliverMaterialRequest(request: MaterialRequest, adminNotes: String? = null) {
        viewModelScope.launch {
            // Validación previa
            if (request.status != RequestStatus.APROBADO) {
                // Podrías emitir un evento de error aquí
                return@launch
            }

            // Necesitamos el ID de la Bodega Central
            // Lo obtenemos consultando las bodegas
            val warehousesResult = inventoryRepository.getWarehouses()

            // Como es un Flow, necesitamos recoger el primer valor
            warehousesResult.collect { result ->
                if (result is Resource.Success) {
                    val centralWarehouse = result.data?.find { it.type == WarehouseType.FIXED }

                    if (centralWarehouse == null) {
                        // Error: No se encontró la bodega central
                        return@collect
                    }

                    // Llamar a la función de entrega
                    inventoryRepository.deliverMaterialRequest(
                        requestId = request.id,
                        centralWarehouseId = centralWarehouse.id,
                        adminNotes = adminNotes
                    )

                    // La UI se actualizará automáticamente por el Flow
                    return@collect // Salimos después del primer resultado
                }
            }
        }
    }

}