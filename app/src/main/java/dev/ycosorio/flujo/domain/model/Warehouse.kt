package dev.ycosorio.flujo.domain.model

import com.google.firebase.firestore.DocumentId

/**
 * Representa una bodega, ya sea física o móvil, donde se almacenan materiales.
 *
 * @property id El identificador único de la bodega, correspondiente al ID del documento en Firestore.
 * @property name El nombre descriptivo de la bodega (por ejemplo, "Bodega Central", "Camioneta Ford-150").
 * @property type El tipo de bodega, que puede ser `FIXED` (fija) o `MOBILE` (móvil).
 */
data class Warehouse(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val type: WarehouseType = WarehouseType.MOBILE
)