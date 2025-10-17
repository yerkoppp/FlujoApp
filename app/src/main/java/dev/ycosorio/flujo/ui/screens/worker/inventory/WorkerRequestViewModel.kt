package dev.ycosorio.flujo.ui.screens.worker.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WorkerRequestViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val userRepository: UserRepository // Necesitamos saber quién es el usuario actual
) : ViewModel() {

    private val _myRequestsState = MutableStateFlow<Resource<List<MaterialRequest>>>(Resource.Loading())
    val myRequestsState = _myRequestsState.asStateFlow()

    private val _createRequestState = MutableStateFlow<Resource<Unit>>(Resource.Success(Unit))
    val createRequestState = _createRequestState.asStateFlow()

    // Suponemos que tenemos el ID del usuario actual. En una app real, lo obtendríamos de Firebase Auth.
    private val currentUserId = "id_del_usuario_actual" // Esto lo reemplazaremos con la autenticación real

    init {
        loadMyRequests()
    }

    private fun loadMyRequests() {
        inventoryRepository.getRequestsForWorker(currentUserId)
            .onEach { result ->
                _myRequestsState.value = result
            }.launchIn(viewModelScope)
    }

    fun createMaterialRequest(materialId: String, materialName: String, quantity: Int) {
        viewModelScope.launch {
            _createRequestState.value = Resource.Loading()

            // Simulamos obtener el nombre del trabajador. En la app real, lo tendríamos del perfil.
            val workerName = "Nombre del Trabajador"

            val newRequest = MaterialRequest(
                id = UUID.randomUUID().toString(), // Generamos un ID único
                workerId = currentUserId,
                workerName = workerName,
                materialId = materialId,
                materialName = materialName,
                quantity = quantity,
                status = RequestStatus.PENDIENTE,
                requestDate = Date()
            )

            _createRequestState.value = inventoryRepository.createMaterialRequest(newRequest)
        }
    }
}