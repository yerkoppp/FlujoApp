package dev.ycosorio.flujo.domain.model

import com.google.firebase.firestore.DocumentId

/**
 * Representa la cantidad de un material específico en una bodega específica.
 * Este documento vivirá dentro de una sub-colección "stock" en un "Warehouse".
 * Ej: /warehouses/{warehouseId}/stock/{stockItemId}
 */
data class StockItem(
    @DocumentId
    val id: String = "",       // ID del documento de stock
    val materialId: String = "", // ID del Material (de la colección 'materials')
    val materialName: String = "", // Nombre duplicado para búsquedas fáciles
    val quantity: Int = 0
)