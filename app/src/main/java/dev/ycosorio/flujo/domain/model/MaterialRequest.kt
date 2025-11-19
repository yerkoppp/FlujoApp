package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa una solicitud de materiales realizada por un trabajador a una bodega.
 *
 * @property id El identificador único de la solicitud.
 * @property workerId El ID del trabajador que realiza la solicitud.
 * @property workerName El nombre del trabajador.
 * @property warehouseId El ID de la bodega a la que se le solicitan los materiales.
 * @property items La lista de materiales y cantidades solicitadas.
 * @property status El estado actual de la solicitud (Pendiente, Aprobado, etc.).
 * @property requestDate La fecha en que se creó la solicitud.
 * @property approvalDate La fecha en que se aprobó la solicitud. Nulo si no ha sido aprobada.
 * @property rejectionDate La fecha en que se rechazó la solicitud. Nulo si no ha sido rechazada.
 * @property deliveryDate La fecha en que se entregaron los materiales. Nulo si no han sido entregados.
 * @property adminNotes Notas opcionales dejadas por el administrador que revisó la solicitud.
 */
data class MaterialRequest(
    val id: String,
    val workerId: String,
    val workerName: String,
    val warehouseId: String,
    val items: List<RequestItem>,
    val status: RequestStatus,
    val requestDate: Date,
    val approvalDate: Date? = null,
    val rejectionDate: Date? = null,
    val deliveryDate: Date? = null,
    val adminNotes: String? = null
)


/**
 * Representa un material individual dentro de una solicitud.
 *
 * @property materialId El identificador único del material.
 * @property materialName El nombre del material.
 * @property quantity La cantidad solicitada de este material.
 */
data class RequestItem(
    val materialId: String,
    val materialName: String,
    val quantity: Int
)