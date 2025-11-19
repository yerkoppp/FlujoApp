package dev.ycosorio.flujo.domain.model
import com.google.firebase.firestore.DocumentId

/**
 * Representa un vehículo de la empresa que puede ser asignado a uno o varios trabajadores.
 *
 * @property id El identificador único del vehículo, correspondiente al ID del documento en Firestore.
 * @property plate La patente o placa del vehículo.
 * @property description Una descripción del vehículo, como marca, modelo, año y color.
 * @property userIds Una lista con los identificadores únicos (UIDs) de los usuarios que tienen asignado este vehículo.
 * @property maxUsers El número máximo de usuarios que pueden ser asignados a este vehículo simultáneamente.
 * @property assignedWarehouseId El ID opcional de la bodega a la que está asignado el vehículo. Es nulo si no está asignado a ninguna bodega específica.
 */
data class Vehicle(
    @DocumentId
    val id: String = "",
    val plate: String = "",
    val description: String = "",
    val userIds: List<String> = emptyList(),
    val maxUsers: Int = 6,
    val assignedWarehouseId: String? = null
)