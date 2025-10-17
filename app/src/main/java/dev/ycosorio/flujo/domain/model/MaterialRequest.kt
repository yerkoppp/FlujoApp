package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa una solicitud de materiales hecha por un trabajador.
 *
 * @property id El ID único de la solicitud.
 * @property workerId El ID del trabajador que hace la solicitud.
 * @property workerName El nombre del trabajador (para mostrar en la UI).
 * @property materialId El ID del material solicitado.
 * @property materialName El nombre del material (para mostrar en la UI).
 * @property quantity La cantidad solicitada.
 * @property status El estado actual de la solicitud (Pendiente, Aprobado, etc.).
 * @property requestDate La fecha en que se creó la solicitud.
 * @property approvalDate La fecha en que se aprobó/rechazó la solicitud.
 * @property pickupDate La fecha asignada para el retiro.
 */
data class MaterialRequest(
    val id: String,
    val workerId: String,
    val workerName: String,
    val materialId: String,
    val materialName: String,
    val quantity: Int,
    val status: RequestStatus,
    val requestDate: Date,
    val approvalDate: Date? = null,
    val pickupDate: Date? = null
)