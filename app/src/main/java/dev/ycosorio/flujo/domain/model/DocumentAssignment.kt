package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa el estado de un documento asignado.
 */
enum class DocumentStatus {
    /**
     * Indica que el documento está pendiente de ser firmado por el trabajador.
     */
    PENDIENTE,
    /**
     * Indica que el documento ya ha sido firmado por el trabajador.
     */
    FIRMADO
}

/**
 * Representa la asignación de un documento a un trabajador específico.
 *
 * Esta clase contiene todos los detalles sobre un documento que ha sido asignado,
 * incluyendo su estado, las fechas relevantes y la información del trabajador.
 *
 * @property id El identificador único de esta asignación de documento.
 * @property templateId El ID de la plantilla de documento a partir de la cual se generó este documento.
 * @property documentTitle El título del documento asignado.
 * @property documentFileUrl La URL donde se puede descargar o visualizar el archivo del documento (por ejemplo, un PDF).
 * @property workerId El identificador único del trabajador al que se le ha asignado el documento.
 * @property workerName El nombre del trabajador asignado. Puede cargarse por separado.
 * @property status El estado actual de la asignación (PENDIENTE o FIRMADO).
 * @property assignedDate La fecha y hora en que se asignó el documento.
 * @property signedDate La fecha y hora en que se firmó el documento. Es nulo si aún no se ha firmado.
 * @property signatureUrl La URL de la imagen que contiene la firma del trabajador. Es nulo si aún no se ha firmado.
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