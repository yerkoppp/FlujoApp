package dev.ycosorio.flujo.domain.repository

import android.net.Uri
import dev.ycosorio.flujo.domain.model.ExpenseReport
import dev.ycosorio.flujo.domain.model.ExpenseReportStatus
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {

    /**
     * Obtiene las rendiciones de un trabajador específico
     */
    fun getExpenseReportsForWorker(workerId: String): Flow<Resource<List<ExpenseReport>>>

    /**
     * Obtiene todas las rendiciones (para admin)
     */
    fun getAllExpenseReports(): Flow<Resource<List<ExpenseReport>>>

    /**
     * Obtiene una rendición por ID
     */
    fun getExpenseReportById(reportId: String): Flow<Resource<ExpenseReport>>

    /**
     * Crea o actualiza una rendición (para guardar borrador)
     */
    suspend fun saveExpenseReport(report: ExpenseReport): Resource<String>

    /**
     * Sube una imagen de un comprobante y retorna la URL
     */
    suspend fun uploadExpenseImage(workerId: String, imageUri: Uri): Resource<String>

    /**
     * Cambia el estado de una rendición
     */
    suspend fun updateExpenseReportStatus(
        reportId: String,
        status: ExpenseReportStatus,
        adminNotes: String? = null
    ): Resource<Unit>

    /**
     * Elimina una rendición borrador
     */
    suspend fun deleteExpenseReport(reportId: String): Resource<Unit>
}