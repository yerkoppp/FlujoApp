package dev.ycosorio.flujo.ui.screens.worker.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.utils.SimulationAuth
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
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _myRequestsState = MutableStateFlow<Resource<List<MaterialRequest>>>(Resource.Loading())
    val myRequestsState = _myRequestsState.asStateFlow()

    private val _createRequestState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val createRequestState = _createRequestState.asStateFlow()

    // Suponemos que tenemos el ID del usuario actual. En una app real, lo obtendríamos de Firebase Auth.
    //private val _currentUserId = SimulationAuth.currentUserId // Esto lo reemplazaremos con la autenticación real
    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            /*_currentUserId.collect { userId ->
                loadMyRequests(userId)
            }*/
            authRepository.currentUser.collect { authUser -> // <-- CAMBIAR
                currentUserId = authUser?.uid
                authUser?.let { loadMyRequests(it.uid) }
            }
        }
    }

    private fun loadMyRequests(userId: String) {
        inventoryRepository.getRequestsForWorker(userId)
            .onEach { result ->
                _myRequestsState.value = result
            }.launchIn(viewModelScope)
    }

    fun createMaterialRequest(materialId: String, materialName: String, quantity: Int) {
        //val currentUserId = _currentUserId.value
        val userId = currentUserId ?: return

        viewModelScope.launch {
            // --- VALIDACIONES ---
            if (materialName.isBlank()) {
                _createRequestState.value = Resource.Error("El nombre del material no puede estar vacío.")
                return@launch
            }
            if (quantity <= 0) {
                _createRequestState.value = Resource.Error("La cantidad debe ser mayor que cero.")
                return@launch
            }

            _createRequestState.value = Resource.Loading()

            // Simulamos obtener el nombre del trabajador.
            val userResource = userRepository.getUser(userId)
            val workerName = if (userResource is Resource.Success) {
                userResource.data?.name ?: "Trabajador"
            } else {
                "Trabajador"
            }

            val newRequest = MaterialRequest(
                id = UUID.randomUUID().toString(), // Generamos un ID único
                workerId = userId,
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

    /**
     * Resetea el estado de creación a Idle. Útil después de mostrar un error.
     */
    fun resetCreateState() {
        _createRequestState.value = Resource.Idle()
    }
}