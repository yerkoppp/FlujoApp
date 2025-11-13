package dev.ycosorio.flujo.domain.model

import java.util.Date

/**
 * Representa un ítem individual de gasto (boleta o factura)
 */
data class ExpenseItem(
    val id: String = "",
    val imageUrl: String = "",
    val reason: String = "",
    val documentNumber: String = "",
    val amount: Double = 0.0,
    val uploadDate: Date = Date()
)