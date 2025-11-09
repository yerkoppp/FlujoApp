package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa el estado de un documento asignado.
 */
enum class DocumentStatus {
    PENDIENTE,
    FIRMADO
}

/**
 * Representa la asignación de un documento a un trabajador específico.
 *
 * @property id El ID único de esta asignación.
 * @property templateId El ID de la plantilla de documento utilizada.
 * @property documentTitle El título del documento (desnormalizado para la UI).
 * @property workerId El ID del trabajador al que se le asignó.
 * @property status El estado actual del documento (Pendiente o Firmado).
 * @property assignedDate La fecha en que se realizó la asignación.
 * @property signedDate La fecha en que el trabajador firmó el documento.
 * @property signatureUrl La URL de la imagen de la firma guardada en Firebase Storage.
 */
data class DocumentAssignment(
    val id: String,
    val templateId: String,
    val documentTitle: String,
    val documentFileUrl: String = "",
    val workerId: String,
    val workerName: String = "",
    val status: DocumentStatus,
    val assignedDate: Date,
    val signedDate: Date? = null,
    val signatureUrl: String? = null
)