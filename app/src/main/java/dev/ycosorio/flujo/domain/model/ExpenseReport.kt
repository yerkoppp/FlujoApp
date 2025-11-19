package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa una rendición de gastos completa, que agrupa varios ítems de gasto individuales.
 *
 * Contiene información sobre el trabajador que la crea, su estado actual, las fechas
 * clave del proceso y el monto total.
 *
 * @property id El identificador único de la rendición. Se genera automáticamente.
 * @property workerId El ID del trabajador que crea la rendición.
 * @property workerName El nombre del trabajador. Puede cargarse por separado.
 * @property items La lista de todos los ítems de gasto (boletas/facturas) incluidos en esta rendición.
 * @property totalAmount La suma total de los montos de todos los ítems.
 * @property status El estado actual del ciclo de vida de la rendición (Borrador, Enviado, etc.).
 * @property createdDate La fecha en que se creó la rendición por primera vez.
 * @property submittedDate La fecha en que el trabajador envió la rendición para su revisión. Es nulo si está en borrador.
 * @property reviewedDate La fecha en que un administrador aprobó o rechazó la rendición. Es nulo si no ha sido revisada.
 * @property adminNotes Comentarios o notas opcionales dejadas por el administrador durante la revisión.
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
    /**
     * La rendición está siendo creada por el trabajador y aún no ha sido enviada para revisión.
     */
    DRAFT,
    /**
     * La rendición ha sido finalizada por el trabajador y enviada a un administrador para su revisión.
     */
    SUBMITTED,

    /**
     * La rendición ha sido revisada y aprobada por un administrador.
     */
    APPROVED,

    /**
     * La rendición ha sido revisada y rechazada por un administrador.
     */
    REJECTED
}