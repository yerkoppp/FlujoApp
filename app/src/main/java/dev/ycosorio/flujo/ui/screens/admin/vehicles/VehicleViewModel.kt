package dev.ycosorio.flujo.ui.screens.admin.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.model.Vehicle
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.domain.repository.VehicleRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Definimos el Estado de la UI (UDF - Unidirectional Data Flow)
data class VehicleManagementUiState(
    val isLoading: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    // Mantenemos una lista completa de usuarios para buscar nombres
    val allWorkers: List<User> = emptyList(),
    // Lista separada de usuarios sin vehículo para los diálogos de asignación
    val unassignedWorkers: List<User> = emptyList(),
    val error: String? = null
)

// 2. ViewModel
@HiltViewModel
class VehicleManagementViewModel @Inject constructor(
    private val vehicleRepo: VehicleRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleManagementUiState())
    val uiState: StateFlow<VehicleManagementUiState> = _uiState.asStateFlow()

    // Usamos SharedFlow para eventos que solo deben consumirse una vez (ej. SnackBar)
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Combinamos los dos flujos.
            // Esto se actualizará automáticamente si cambia un usuario O un vehículo.
            val usersFlow = userRepo.getAllWorkers()
            val vehiclesFlow = vehicleRepo.getVehicles()

            usersFlow.combine(vehiclesFlow) { users: List<User>, vehiclesResource: Resource<List<Vehicle>> ->

                if (vehiclesResource is Resource.Error) {
                    _uiState.update { it.copy(isLoading = false, error = vehiclesResource.message) }
                } else if (vehiclesResource is Resource.Success) {
                    val allWorkers = users
                    val vehicles = vehiclesResource.data ?: emptyList()
                    val unassigned = allWorkers.filter { it.assignedVehicleId == null }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            vehicles = vehicles,
                            allWorkers = allWorkers,
                            unassignedWorkers = unassigned,
                            error = null
                        )
                    }
                }
            }.catch { e ->
                // Captura excepciones inesperadas de los flujos
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Error inesperado en el flujo"
                    )
                }
            }.collect() // Inicia la recolección del flujo combinado
        }
    }

    // --- Funciones de Acción llamadas desde la UI ---

    fun createVehicle(plate: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = vehicleRepo.createVehicle(plate, description)
            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Vehículo creado con éxito"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error al crear vehículo"))
                }
                else -> Unit
            }
            // El estado de isLoading se quitará automáticamente cuando el flujo 'getVehicles' emita el nuevo valor
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = vehicleRepo.deleteVehicle(vehicleId)
            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Vehículo eliminado"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error al eliminar vehículo"))
                }
                else -> Unit
            }
        }
    }

    fun assignUserToVehicle(userId: String, vehicleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = vehicleRepo.assignUserToVehicle(userId, vehicleId)
            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Usuario asignado"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error al asignar usuario"))
                }
                else -> Unit
            }
        }
    }

    fun removeUserFromVehicle(userId: String, vehicleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = vehicleRepo.removeUserFromVehicle(userId, vehicleId)
            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Usuario removido del vehículo"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error al remover usuario"))
                }
                else -> Unit
            }
        }
    }
}