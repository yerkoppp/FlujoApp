package dev.ycosorio.flujo.ui.screens.worker.inventory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.model.StockItem
import dev.ycosorio.flujo.domain.model.Warehouse
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.repository.VehicleRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkerRequestViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    // --- ESTADOS ---
    private val _myRequestsState = MutableStateFlow<Resource<List<MaterialRequest>>>(Resource.Loading())

    private val currentUserId: StateFlow<String?> = authRepository.currentUser
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val myRequestsState: StateFlow<Resource<List<MaterialRequest>>> =
        currentUserId.flatMapLatest { uid ->
            if (uid != null) {
                inventoryRepository.getRequestsForWorker(uid)
            } else {
                flowOf(Resource.Idle())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )

    /**
     * StateFlow que proporciona la bodega asignada al trabajador.
     * Se actualiza automáticamente cuando cambia el usuario o su vehículo asignado.
     */
    private val _myWarehouse = MutableStateFlow<Warehouse?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val myWarehouse: StateFlow<Warehouse?> =
        currentUserId.flatMapLatest { uid ->
            if (uid != null) {
                vehicleRepository.getVehicles().flatMapLatest { vehiclesResult ->
                    if (vehiclesResult is Resource.Success) {
                        val myVehicle = vehiclesResult.data?.find { it.userIds.contains(uid) }
                        val warehouseId = myVehicle?.assignedWarehouseId

                        if (warehouseId != null) {
                            inventoryRepository.getWarehouses().map { warehousesResult ->
                                (warehousesResult as? Resource.Success)?.data?.find { it.id == warehouseId }
                            }
                        } else {
                            flowOf(null)
                        }
                    } else {
                        flowOf(null)
                    }
                }
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    /**
     * Estado privado que mantiene el stock de la bodega del trabajador.
     */
    private val _myWarehouseStock = MutableStateFlow<Resource<List<StockItem>>>(Resource.Idle())

    /**
     * StateFlow que proporciona el stock de la bodega asignada al trabajador.
     * Se actualiza automáticamente cuando cambia la bodega asignada.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val myWarehouseStock: StateFlow<Resource<List<StockItem>>> =
        myWarehouse.flatMapLatest { warehouse ->
            if (warehouse != null) {
                inventoryRepository.getStockForWarehouse(warehouse.id)
            } else {
                flowOf(Resource.Idle())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Idle()
        )

    /**
     * Inicializa el ViewModel cargando las solicitudes del trabajador,
     * su vehículo asignado, la bodega asociada y el stock de dicha bodega
     */
    init {
        authRepository.currentUser.onEach { authUser ->
            authUser?.let { user ->
                // Cargar solicitudes del trabajador
                inventoryRepository.getRequestsForWorker(user.uid).onEach { result ->
                    _myRequestsState.value = result
                }.launchIn(viewModelScope)

                // Cargar el vehículo asignado al trabajador
                vehicleRepository.getVehicles().onEach { vehiclesResult ->
                    if (vehiclesResult is Resource.Success) {
                        val myVehicle = vehiclesResult.data?.find { it.userIds.contains(user.uid) }
                        myVehicle?.assignedWarehouseId?.let { warehouseId ->
                            // Cargar la bodega
                            inventoryRepository.getWarehouses().onEach { warehousesResult ->
                                if (warehousesResult is Resource.Success) {
                                    _myWarehouse.value = warehousesResult.data?.find { it.id == warehouseId }
                                }
                            }.launchIn(viewModelScope)

                            // Cargar el stock de la bodega
                            inventoryRepository.getStockForWarehouse(warehouseId).onEach { stockResult ->
                                _myWarehouseStock.value = stockResult
                            }.launchIn(viewModelScope)
                        }
                    }
                }.launchIn(viewModelScope)
            }
        }.launchIn(viewModelScope)
    }

    /**
     * Cancela una solicitud que está en estado PENDIENTE.
     */
    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val result = inventoryRepository.updateRequestStatus(
                    requestId = requestId,
                    status = RequestStatus.CANCELADO,
                    adminNotes = null
                )

                if (result is Resource.Error) {
                    Log.e("WorkerRequestViewModel", "Error al cancelar: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e("WorkerRequestViewModel", "Excepción al cancelar", e)
            }
        }
    }
}