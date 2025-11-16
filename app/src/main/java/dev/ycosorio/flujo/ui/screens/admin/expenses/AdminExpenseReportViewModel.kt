package dev.ycosorio.flujo.ui.screens.admin.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ycosorio.flujo.domain.model.ExpenseReport
import dev.ycosorio.flujo.domain.model.ExpenseReportStatus
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.repository.ExpenseRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminExpenseUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val workers: List<User> = emptyList()
)

@HiltViewModel
class AdminExpenseReportViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminExpenseUiState())
    val uiState: StateFlow<AdminExpenseUiState> = _uiState.asStateFlow()

    // Filtros
    private val _selectedWorker = MutableStateFlow<String?>(null)
    val selectedWorker: StateFlow<String?> = _selectedWorker.asStateFlow()

    private val _selectedStatus = MutableStateFlow<ExpenseReportStatus?>(null)
    val selectedStatus: StateFlow<ExpenseReportStatus?> = _selectedStatus.asStateFlow()

    // Rendiciones con filtros reactivos
    @OptIn(ExperimentalCoroutinesApi::class)
    val expenseReports: StateFlow<Resource<List<ExpenseReport>>> =
        _selectedWorker.flatMapLatest { workerId ->
            if (workerId != null) {
                // Filtrar por trabajador específico
                expenseRepository.getExpenseReportsForWorker(workerId)
            } else {
                // Todas las rendiciones
                expenseRepository.getAllExpenseReports()
            }
        }.combine(_selectedStatus) { resource, statusFilter ->
            // Aplicar filtro de estado de forma reactiva
            when (resource) {
                is Resource.Success -> {
                    val filtered = resource.data?.let { reports ->
                        if (statusFilter != null) {
                            reports.filter { it.status == statusFilter }
                        } else {
                            reports
                        }
                    } ?: emptyList()
                    Resource.Success(filtered)
                }
                is Resource.Error -> resource
                is Resource.Loading -> resource
                else -> resource
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )

    init {
        loadWorkers()
    }

    private fun loadWorkers() {
        viewModelScope.launch {
            userRepository.getAllWorkers().collect { workers ->
                _uiState.value = _uiState.value.copy(workers = workers)
            }
        }
    }

    fun onWorkerFilterChanged(workerId: String?) {
        _selectedWorker.value = workerId
    }

    fun onStatusFilterChanged(status: ExpenseReportStatus?) {
        _selectedStatus.value = status
    }

    fun approveReport(reportId: String, adminNotes: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = expenseRepository.updateExpenseReportStatus(
                reportId = reportId,
                status = ExpenseReportStatus.APPROVED,
                adminNotes = adminNotes.ifBlank { null }
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun rejectReport(reportId: String, adminNotes: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = expenseRepository.updateExpenseReportStatus(
                reportId = reportId,
                status = ExpenseReportStatus.REJECTED,
                adminNotes = adminNotes
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}