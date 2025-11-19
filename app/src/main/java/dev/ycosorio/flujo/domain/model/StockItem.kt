package dev.ycosorio.flujo.domain.model

import com.google.firebase.firestore.DocumentId

/**
 * Representa un ítem de inventario, es decir, la cantidad de un material específico
 * en una bodega determinada.
 *
 * Este modelo de datos se corresponde con un documento individual dentro de la subcolección
 * `stock` de una bodega en Firestore. La ruta sería, por ejemplo:
 * `/warehouses/{warehouseId}/stock/{stockItemId}`.
 *
 * @property id El identificador único del ítem de stock, que se corresponde con el ID del documento en Firestore.
 * @property materialId El ID del documento del material (desde la colección global `materials`) al que pertenece este stock.
 * @property materialName El nombre del material. Se guarda aquí como una copia para facilitar las búsquedas y visualizaciones sin necesidad de hacer una consulta adicional al documento del material.
 * @property quantity La cantidad disponible de este material en la bodega.
 */
data class StockItem(
    @DocumentId
    val id: String = "",
    val materialId: String = "",
    val materialName: String = "",
    val quantity: Int = 0
)