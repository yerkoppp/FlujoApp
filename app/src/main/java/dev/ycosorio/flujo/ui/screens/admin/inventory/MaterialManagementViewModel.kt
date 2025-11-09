package dev.ycosorio.flujo.ui.screens.admin.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.Material
import dev.ycosorio.flujo.domain.model.StockItem
import dev.ycosorio.flujo.domain.model.Warehouse
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

data class MaterialManagementUiState(
    val materials: List<Material> = emptyList(),
    val warehouses: List<Warehouse> = emptyList(),
    val selectedWarehouse: Warehouse? = null,
    val stockItems: List<StockItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MaterialManagementViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialManagementUiState())
    val uiState: StateFlow<MaterialManagementUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadMaterials()
        loadWarehouses()
    }

    private fun loadMaterials() {
        inventoryRepository.getMaterials().onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            materials = result.data ?: emptyList(),
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Error al cargar materiales"
                        )
                    }
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    private fun loadWarehouses() {
        inventoryRepository.getWarehouses().onEach { result ->
            when (result) {
                is Resource.Success -> {
                    val warehouses = result.data ?: emptyList()
                    _uiState.update { it.copy(warehouses = warehouses) }

                    // Auto-seleccionar la primera bodega si no hay ninguna seleccionada
                    if (_uiState.value.selectedWarehouse == null && warehouses.isNotEmpty()) {
                        selectWarehouse(warehouses.first())
                    }
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Error al cargar bodegas"))
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    fun selectWarehouse(warehouse: Warehouse) {
        _uiState.update { it.copy(selectedWarehouse = warehouse, isLoading = true) }
        loadStockForWarehouse(warehouse.id)
    }

    private fun loadStockForWarehouse(warehouseId: String) {
        inventoryRepository.getStockForWarehouse(warehouseId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            stockItems = result.data ?: emptyList()
                        )
                    }
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Error al cargar stock"))
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    /**
     * Crea una nueva definición de material en el catálogo.
     * No agrega stock automáticamente.
     */
    fun createMaterialDefinition(name: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = inventoryRepository.createMaterialDefinition(name, description)
            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Material creado con éxito"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error al crear"))
                }
                else -> Unit
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Agrega stock de un material a la bodega seleccionada actualmente.
     */
    fun addStockToSelectedWarehouse(material: Material, quantity: Int) {
        viewModelScope.launch {
            val warehouseId = _uiState.value.selectedWarehouse?.id
            if (warehouseId == null) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Selecciona una bodega primero"))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val result = inventoryRepository.addStockToWarehouse(
                warehouseId,
                material,
                quantity
            )
            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Stock agregado exitosamente"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error al agregar stock"))
                }
                else -> Unit
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}