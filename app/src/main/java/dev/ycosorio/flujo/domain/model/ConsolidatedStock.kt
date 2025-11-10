package dev.ycosorio.flujo.domain.model

/**
 * Representa el inventario consolidado de un material sumando todas las bodegas.
 */
data class ConsolidatedStock(
    val materialId: String,
    val materialName: String,
    val totalQuantity: Int,
    val warehouseBreakdown: List<WarehouseStock> = emptyList()
)

/**
 * Representa la cantidad en una bodega específica.
 */
data class WarehouseStock(
    val warehouseId: String,
    val warehouseName: String,
    val quantity: Int
)