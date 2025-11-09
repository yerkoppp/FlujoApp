package dev.ycosorio.flujo.ui.screens.admin.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.Warehouse
import dev.ycosorio.flujo.domain.model.WarehouseType
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WarehouseManagementUiState(
    val isLoading: Boolean = false,
    val warehouses: List<Warehouse> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class WarehouseManagementViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarehouseManagementUiState())
    val uiState: StateFlow<WarehouseManagementUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadWarehouses()
    }

    private fun loadWarehouses() {
        inventoryRepository.getWarehouses().onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            warehouses = result.data ?: emptyList(),
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Error al cargar bodegas"
                        )
                    }
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    fun createWarehouse(name: String, type: WarehouseType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = inventoryRepository.createWarehouse(name, type)
            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Bodega creada con éxito"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error al crear bodega"))
                }
                else -> Unit
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}