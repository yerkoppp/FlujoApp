package dev.ycosorio.flujo.domain.model

/**
 * Representa el inventario consolidado de un material, sumando las existencias de todas las bodegas.
 *
 * @property materialId El identificador único del material.
 * @property materialName El nombre del material.
 * @property totalQuantity La cantidad total del material sumando todas las bodegas.
 * @property warehouseBreakdown Una lista que desglosa el inventario por cada bodega individual.
 */
data class ConsolidatedStock(
    val materialId: String,
    val materialName: String,
    val totalQuantity: Int,
    val warehouseBreakdown: List<WarehouseStock> = emptyList()
)


/**
 * Representa la cantidad de un material en una bodega específica.
 *
 * @property warehouseId El identificador único de la bodega.
 * @property warehouseName El nombre de la bodega.
 * @property quantity La cantidad del material en esta bodega.
 */
data class WarehouseStock(
    val warehouseId: String,
    val warehouseName: String,
    val quantity: Int
)