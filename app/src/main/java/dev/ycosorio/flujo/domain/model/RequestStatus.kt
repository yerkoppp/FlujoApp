package dev.ycosorio.flujo.domain.model

/**
 * Representa el estado de una solicitud de material.
 */
enum class RequestStatus {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    RETIRADO
}