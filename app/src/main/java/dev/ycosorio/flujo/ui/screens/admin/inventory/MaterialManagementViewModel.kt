package dev.ycosorio.flujo.ui.screens.admin.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.InventoryItem
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
    val isLoading: Boolean = false,
    val materials: List<InventoryItem> = emptyList(),
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
    }

    private fun loadMaterials() {
        inventoryRepository.getAvailableMaterials().onEach { result ->
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
                            error = result.message ?: "Error desconocido"
                        )
                    }
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    fun createMaterial(name: String, initianQuantity: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = inventoryRepository.createMaterial(name, initianQuantity)
            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Material creado con éxito"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error al crear"))
                }
                else -> Unit
            }
            // El 'isLoading' se quitará cuando el 'loadMaterials' emita la nueva lista
        }
    }

    fun addStock(itemId: String, amount: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = inventoryRepository.addStockToMaterial(itemId, amount)
            when (result) {
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Stock actualizado"))
                }
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Error al añadir stock"))
                }
                else -> Unit
            }
        }
    }
}