package dev.ycosorio.flujo.domain.model

/**
 * Define el tipo de una bodega, diferenciando entre ubicaciones físicas y móviles.
 */
enum class WarehouseType {
    /**
     * Una bodega física y permanente, como un almacén central o una sucursal.
     */
    FIXED,
    /**
     * Una bodega móvil, como una camioneta o un contenedor de herramientas,
     * que puede cambiar de ubicación y está asociada a un vehículo o proyecto.
     */
    MOBILE
}