package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa una solicitud de materiales hecha por un trabajador.
 *
 * @property id El ID único de la solicitud.
 * @property workerId El ID del trabajador que hace la solicitud.
 * @property workerName El nombre del trabajador (para mostrar en la UI).
 * @property warehouseId El ID de la bodega DESTINO (bodega móvil del trabajador).
 * @property materialId El ID del material solicitado.
 * @property materialName El nombre del material (para mostrar en la UI).
 * @property quantity La cantidad solicitada.
 * @property status El estado actual de la solicitud.
 * @property requestDate La fecha en que se creó la solicitud.
 * @property approvalDate La fecha en que se aprobó la solicitud.
 * @property rejectionDate La fecha en que se rechazó la solicitud.
 * @property deliveryDate La fecha en que se entregó el material.
 * @property adminNotes Notas del administrador sobre la solicitud.
 */
data class MaterialRequest(
    val id: String,
    val workerId: String,
    val workerName: String,
    val warehouseId: String,
    val materialId: String,
    val materialName: String,
    val quantity: Int,
    val status: RequestStatus,
    val requestDate: Date,
    val approvalDate: Date? = null,
    val rejectionDate: Date? = null,
    val deliveryDate: Date? = null,
    val adminNotes: String? = null
)