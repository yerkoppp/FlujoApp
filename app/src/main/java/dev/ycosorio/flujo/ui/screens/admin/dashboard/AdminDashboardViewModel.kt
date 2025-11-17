package dev.ycosorio.flujo.ui.screens.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.DocumentAssignment
import dev.ycosorio.flujo.domain.model.MaterialRequest
import dev.ycosorio.flujo.domain.model.Message
import dev.ycosorio.flujo.domain.model.RequestStatus
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.DocumentRepository
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.repository.MessageRepository
import dev.ycosorio.flujo.ui.screens.worker.dashboard.WorkerDashboardUiState
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


data class AdminDashboardUiState(
    val pendingDocuments: List<DocumentAssignment> = emptyList(),
    val recentRequests: List<MaterialRequest> = emptyList(),
    val messagesUnread: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val documentRepository: DocumentRepository,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    // Usamos flatMapLatest para reaccionar al cambio de usuario (login/logout)
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AdminDashboardUiState> = authRepository.currentUser
        .flatMapLatest { authUser ->
            if (authUser == null) {
                // Si no hay usuario, emitimos un estado vacío
                flowOf(AdminDashboardUiState(isLoading = false, error = "Usuario no autenticado"))
            } else {
                // Si hay usuario, combinamos los flujos de documentos y solicitudes
                val documentsFlow = documentRepository.getAssignedDocumentsForUser(authUser.uid)
                val requestsFlow = inventoryRepository.getRequestsForWorker(authUser.uid)
                val messagesFlow = messageRepository.getReceivedMessages(authUser.uid)

                combine(documentsFlow, requestsFlow, messagesFlow) { docResult, reqResult, mesResult ->
                    val documents = (docResult as? Resource.Success)?.data ?: emptyList()
                    val requests = (reqResult as? Resource.Success)?.data ?: emptyList()
                    val messages = (mesResult as? Resource.Success)?.data ?: emptyList()

                    val isLoading = docResult is Resource.Loading || reqResult is Resource.Loading || mesResult is Resource.Loading
                    val error = (docResult as? Resource.Error)?.message
                        ?: (reqResult as? Resource.Error)?.message

                    AdminDashboardUiState(
                        // Filtramos solo los documentos que el trabajador NO ha firmado
                        pendingDocuments = documents.filter { it.status.name == RequestStatus.PENDIENTE.name },
                        // Tomamos solo las 5 solicitudes más recientes
                        recentRequests = requests.sortedByDescending { it.requestDate }.take(5),
                        // Filtramos solo los mensajes no leídos
                        messagesUnread = messages.filter { !(it.isRead[authUser.uid] ?: false) },
                        isLoading = isLoading,
                        error = error
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdminDashboardUiState(isLoading = true)
        )
}