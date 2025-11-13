package dev.ycosorio.flujo.ui.screens.worker.expenses

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.ExpenseItem
import dev.ycosorio.flujo.domain.model.ExpenseReport
import dev.ycosorio.flujo.domain.model.ExpenseReportStatus
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.ExpenseRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class ExpenseReportUiState(
    val currentReport: ExpenseReport? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUploadingImage: Boolean = false,
    val uploadProgress: String? = null,
    val reportsList: List<ExpenseReport> = emptyList(),
    val isLoadingList: Boolean = false
)

@HiltViewModel
class ExpenseReportViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseReportUiState())
    val uiState: StateFlow<ExpenseReportUiState> = _uiState.asStateFlow()

    private val currentUserId = authRepository.getCurrentUser()

    init {
        loadExpenseReports()
    }

    /**
     * Carga todas las rendiciones del trabajador
     */
    fun loadExpenseReports() {
        val userId = currentUserId?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingList = true) }

            expenseRepository.getExpenseReportsForWorker(userId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                reportsList = result.data ?: emptyList(),
                                isLoadingList = false
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                error = result.message,
                                isLoadingList = false
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoadingList = true) }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Carga una rendición específica o crea una nueva
     */
    fun loadReport(reportId: String?) {
        viewModelScope.launch {
            if (reportId != null) {
                // Cargar rendición existente
                _uiState.update { it.copy(isLoading = true) }

                expenseRepository.getExpenseReportById(reportId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.update {
                                it.copy(
                                    currentReport = result.data,
                                    isLoading = false
                                )
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    error = result.message,
                                    isLoading = false
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        else -> {}
                    }
                }
            } else {
                // Crear nueva rendición
                createNewDraft()
            }
        }
    }

    /**
     * Crea un nuevo borrador
     */
    private suspend fun createNewDraft() {
        val userId = currentUserId?.uid ?: return

        _uiState.update { it.copy(isLoading = true) }

        val newReport = ExpenseReport(
            id = "",
            workerId = userId,
            workerName = currentUserId?.displayName ?: "",
            items = emptyList(),
            totalAmount = 0.0,
            status = ExpenseReportStatus.DRAFT,
            createdDate = Date()
        )

        when (val result = expenseRepository.saveExpenseReport(newReport)) {
            is Resource.Success -> {
                // ✅ Cargar el draft con el ID que acabamos de crear
                // El repository debe retornar el ID generado
                _uiState.update {
                    it.copy(
                        currentReport = newReport.copy(id = result.data ?: ""),
                        isLoading = false
                    )
                }
            }
            is Resource.Error -> {
                _uiState.update {
                    it.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
            }
            else -> {}
        }
    }

    /**
     * Agrega un nuevo item de gasto
     */
    fun addExpenseItem(
        imageUri: Uri,
        reason: String,
        documentNumber: String,
        amount: Double
    ) {
        viewModelScope.launch {
            val userId = currentUserId?.uid ?: return@launch
            val currentReport = _uiState.value.currentReport

            if (currentReport == null) {
                _uiState.update { it.copy(error = "No hay rendición activa") }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isUploadingImage = true,
                    uploadProgress = "Subiendo imagen..."
                )
            }

            // Subir imagen
            when (val uploadResult = expenseRepository.uploadExpenseImage(userId, imageUri)) {
                is Resource.Success -> {
                    val imageUrl = uploadResult.data!!

                    // Crear nuevo item
                    val newItem = ExpenseItem(
                        id = UUID.randomUUID().toString(),
                        imageUrl = imageUrl,
                        reason = reason,
                        documentNumber = documentNumber,
                        amount = amount,
                        uploadDate = Date()
                    )

                    // Actualizar rendición
                    val updatedItems = currentReport.items + newItem
                    val updatedTotal = updatedItems.sumOf { it.amount }
                    val updatedReport = currentReport.copy(
                        items = updatedItems,
                        totalAmount = updatedTotal
                    )

                    // Guardar
                    when (val saveResult = expenseRepository.saveExpenseReport(updatedReport)) {
                        is Resource.Success -> {
                            val reportId = saveResult.data ?: updatedReport.id
                            _uiState.update {
                                it.copy(
                                    currentReport = updatedReport.copy(id = reportId),
                                    isUploadingImage = false,
                                    uploadProgress = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    isUploadingImage = false,
                                    uploadProgress = null,
                                    error = saveResult.message
                                )
                            }
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            uploadProgress = null,
                            error = uploadResult.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Elimina un item de la rendición
     */
    fun removeExpenseItem(itemId: String) {
        viewModelScope.launch {
            val currentReport = _uiState.value.currentReport ?: return@launch

            _uiState.update { it.copy(isLoading = true) }

            val updatedItems = currentReport.items.filter { it.id != itemId }
            val updatedTotal = updatedItems.sumOf { it.amount }
            val updatedReport = currentReport.copy(
                items = updatedItems,
                totalAmount = updatedTotal
            )

            when (val result = expenseRepository.saveExpenseReport(updatedReport)) {
                is Resource.Success -> {
                    val reportId = result.data ?: updatedReport.id
                    _uiState.update {
                        it.copy(
                            currentReport = updatedReport.copy(id = reportId),
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Envía la rendición para revisión
     */
    fun submitReport() {
        viewModelScope.launch {
            val currentReport = _uiState.value.currentReport ?: return@launch

            if (currentReport.items.isEmpty()) {
                _uiState.update { it.copy(error = "Debe agregar al menos un comprobante") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            when (val result = expenseRepository.updateExpenseReportStatus(
                reportId = currentReport.id,
                status = ExpenseReportStatus.SUBMITTED
            )) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            currentReport = null,
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Elimina una rendición borrador
     */
    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = expenseRepository.deleteExpenseReport(reportId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentReport = if (it.currentReport?.id == reportId) null else it.currentReport
                        )
                    }
                    loadExpenseReports()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearCurrentReport() {
        _uiState.update { it.copy(currentReport = null) }
    }
}