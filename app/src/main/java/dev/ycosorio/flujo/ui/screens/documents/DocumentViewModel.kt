package dev.ycosorio.flujo.ui.screens.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.DocumentAssignment
import dev.ycosorio.flujo.domain.model.DocumentTemplate
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
import android.net.Uri

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

    private val _allAssignmentsForWorker = MutableStateFlow<Resource<List<DocumentAssignment>>>(Resource.Idle())
    val allAssignmentsForWorker = _allAssignmentsForWorker.asStateFlow()

    private val _signedAssignments = MutableStateFlow<Resource<List<DocumentAssignment>>>(Resource.Idle())
    val signedAssignments = _signedAssignments.asStateFlow()

    private val _allWorkers = MutableStateFlow<Resource<List<User>>>(Resource.Idle())
    val allWorkers = _allWorkers.asStateFlow()

    private val _assignmentState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val assignmentState = _assignmentState.asStateFlow()

    private val _uploadState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val uploadState = _uploadState.asStateFlow()

    private val _deleteState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val deleteState = _deleteState.asStateFlow()



    init {
        viewModelScope.launch {

            authRepository.currentUser.collect { authUser ->
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

            userRepository.getAllWorkers().onEach { workers ->
                _allWorkers.value = Resource.Success(workers)
            }.launchIn(viewModelScope)

        } else if (user.role == Role.TRABAJADOR) {
            // Cargar datos para el Trabajador
            documentRepository.getPendingAssignmentsForWorker(user.uid).onEach {
                _pendingAssignments.value = it
            }.launchIn(viewModelScope)

            // Cargar TODOS los documentos asignados al trabajador (pendientes + firmados)
            documentRepository.getAssignedDocumentsForUser(user.uid).onEach {
                _allAssignmentsForWorker.value = it
            }.launchIn(viewModelScope)
           // Cargar documentos firmados
            documentRepository.getSignedDocumentsForWorker(user.uid).onEach {
                _signedAssignments.value = it
            }.launchIn(viewModelScope)
        }
    }

    fun assignDocumentToWorkers(template: DocumentTemplate, workerIds: List<String>) {
        viewModelScope.launch {
            _assignmentState.value = Resource.Loading()
            if (workerIds.isEmpty()) {
                _assignmentState.value = Resource.Error("Selecciona al menos un trabajador.")
                return@launch
            }

            // Filtrar la lista de workers para obtener solo los seleccionados
            val allWorkersData = _allWorkers.value
            if (allWorkersData is Resource.Success) {
                val selectedWorkers = allWorkersData.data?.filter { it.uid in workerIds } ?: emptyList()
                _assignmentState.value = documentRepository.assignDocument(template, selectedWorkers)
            } else {
                _assignmentState.value = Resource.Error("No se pudo cargar la lista de trabajadores.")
            }
        }
    }

    fun resetAssignmentState() {
        _assignmentState.value = Resource.Idle()
    }

    fun uploadTemplate(title: String, fileUri: Uri?) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _uploadState.value = Resource.Error("El título no puede estar vacío.")
                return@launch
            }
            if (fileUri == null || fileUri == Uri.EMPTY) {
                _uploadState.value = Resource.Error("Debes seleccionar un archivo.")
                return@launch
            }

            _uploadState.value = Resource.Loading()
            _uploadState.value = documentRepository.uploadTemplate(title, fileUri)
        }
    }

    fun resetUploadState() {
        _uploadState.value = Resource.Idle()
    }

    fun deleteTemplate(template: DocumentTemplate) {
        viewModelScope.launch {
            _deleteState.value = Resource.Loading()
            _deleteState.value = documentRepository.deleteTemplate(template)
        }
    }

    fun resetDeleteState() {
        _deleteState.value = Resource.Idle()
    }
}