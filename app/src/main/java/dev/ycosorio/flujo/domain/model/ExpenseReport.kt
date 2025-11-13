package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa una rendición de gastos
 */
data class ExpenseReport(
    val id: String = "",
    val workerId: String = "",
    val workerName: String = "",
    val items: List<ExpenseItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: ExpenseReportStatus = ExpenseReportStatus.DRAFT,
    val createdDate: Date = Date(),
    val submittedDate: Date? = null,
    val reviewedDate: Date? = null,
    val adminNotes: String? = null
)

enum class ExpenseReportStatus {
    DRAFT,      // Borrador
    SUBMITTED,  // Enviado
    APPROVED,   // Aprobado
    REJECTED    // Rechazado
}