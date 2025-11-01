package dev.ycosorio.flujo.ui.screens.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.DocumentAssignment
import dev.ycosorio.flujo.domain.model.DocumentTemplate
import dev.ycosorio.flujo.utils.SimulationAuth
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.DocumentRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- Estado para el Usuario Actual ---
    private val _userState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val userState = _userState.asStateFlow()

    // --- Estados para el Administrador ---
    private val _templates = MutableStateFlow<Resource<List<DocumentTemplate>>>(Resource.Idle())
    val templates = _templates.asStateFlow()

    private val _allAssignments = MutableStateFlow<Resource<List<DocumentAssignment>>>(Resource.Idle())
    val allAssignments = _allAssignments.asStateFlow()

    // --- Estados para el Trabajador ---
    private val _pendingAssignments = MutableStateFlow<Resource<List<DocumentAssignment>>>(Resource.Idle())
    val pendingAssignments = _pendingAssignments.asStateFlow()

    // ID de usuario (cambio de role)
    private val _currentUserId = SimulationAuth.currentUserId

    init {
        viewModelScope.launch {
           /* _currentUserId.collect { userId ->
                loadCurrentUser(userId)
            }*/

            authRepository.currentUser.collect { authUser -> // <-- CAMBIAR
                authUser?.let { loadCurrentUser(it.uid) }
            }
        }
    }

    private fun loadCurrentUser(userId: String) {
        viewModelScope.launch {
            _userState.value = Resource.Loading()
            val result = userRepository.getUser(userId)
            _userState.value = result

            // Si el usuario se carga exitosamente, cargamos los datos según su rol
            if (result is Resource.Success && result.data != null) {
                loadDataForRole(result.data)
            }
        }
    }

    private fun loadDataForRole(user: User) {
        if (user.role == Role.ADMINISTRADOR) {
            // Cargar datos para el Admin
            documentRepository.getDocumentTemplates().onEach {
                _templates.value = it
            }.launchIn(viewModelScope)

            documentRepository.getAllAssignments().onEach {
                _allAssignments.value = it
            }.launchIn(viewModelScope)

        } else if (user.role == Role.TRABAJADOR) {
            // Cargar datos para el Trabajador
            documentRepository.getPendingAssignmentsForWorker(user.uid).onEach {
                _pendingAssignments.value = it
            }.launchIn(viewModelScope)
        }
    }
}