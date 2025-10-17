package dev.ycosorio.flujo.domain.model

/**
 * Representa un item individual en el inventario.
 *
 * @property id El ID único del material.
 * @property name El nombre del material (ej: "Cable de red Cat6").
 * @property quantity La cantidad disponible.
 * @property locationId El ID de la ubicación donde se encuentra (ej: "almacen_central" o "CAM-04").
 */
data class InventoryItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val locationId: String
)