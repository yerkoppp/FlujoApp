package dev.ycosorio.flujo.domain.model

/**
 * Define los posibles estados de una solicitud de materiales.
 */
enum class RequestStatus {
    /**
     * La solicitud ha sido creada por el trabajador y está pendiente de revisión.
     */
    PENDIENTE,
    /**
     * La solicitud ha sido aprobada por un administrador y está lista para ser preparada.
     */
    APROBADO,
    /**
     * La solicitud ha sido rechazada por un administrador.
     */
    RECHAZADO,
    /**
     * Los materiales han sido entregados al trabajador.
     */
    ENTREGADO,
    /**
     * La solicitud ha sido cancelada por el trabajador.
     */
    CANCELADO
}