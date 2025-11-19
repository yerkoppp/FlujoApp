package dev.ycosorio.flujo.domain.model

import com.google.firebase.firestore.DocumentId

/**
 * Representa un material o insumo que se maneja en el inventario.
 *
 * @property id El identificador único del material, que se corresponde con el ID del documento en Firestore.
 * @property name El nombre del material (por ejemplo, "Martillo", "Tornillos 2 pulgadas").
 * @property description Una descripción más detallada sobre el material, como sus características o uso.
 */
data class Material(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = ""
)