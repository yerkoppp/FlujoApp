package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa un ítem individual de gasto, como una boleta o una factura,
 * que forma parte de una rendición de gastos más grande.
 *
 * @property id El identificador único del ítem de gasto. Se genera automáticamente.
 * @property imageUrl La URL de la imagen del comprobante (boleta/factura).
 * @property reason Una descripción o motivo del gasto.
 * @property documentNumber El número de folio o identificador del documento (boleta/factura).
 * @property amount El monto total del gasto.
 * @property uploadDate La fecha en que se registró o subió el gasto. Por defecto es la fecha actual.
 */
data class ExpenseItem(
    val id: String = "",
    val imageUrl: String = "",
    val reason: String = "",
    val documentNumber: String = "",
    val amount: Double = 0.0,
    val uploadDate: Date = Date()
)