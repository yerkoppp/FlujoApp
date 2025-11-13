package dev.ycosorio.flujo.ui.screens.worker.inventory

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.repository.VehicleRepository
import dev.ycosorio.flujo.domain.model.Warehouse
import dev.ycosorio.flujo.domain.model.StockItem
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.viewModelScope
import dev.ycosorio.flujo.domain.model.RequestStatus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

@HiltViewModel
class WorkerRequestViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    // --- ESTADOS ---
    private val _myRequestsState = MutableStateFlow<Resource<List<MaterialRequest>>>(Resource.Loading())
    val myRequestsState = _myRequestsState.asStateFlow()

    private val _myWarehouseStock = MutableStateFlow<Resource<List<StockItem>>>(Resource.Idle())
    val myWarehouseStock = _myWarehouseStock.asStateFlow()

    private val _myWarehouse = MutableStateFlow<Warehouse?>(null)
    val myWarehouse = _myWarehouse.asStateFlow()

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