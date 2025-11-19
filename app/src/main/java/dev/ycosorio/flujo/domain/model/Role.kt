package dev.ycosorio.flujo.domain.model

/**
 * Define los roles que un usuario puede tener dentro del sistema.
 * Cada rol tiene diferentes niveles de permisos y acceso.
 */
enum class Role {
    /**
     * Rol estándar para trabajadores, con acceso limitado a sus propias tareas y datos.
     */
    TRABAJADOR,
    /**
     * Rol de administrador con acceso total a todas las funcionalidades.
     */
    ADMINISTRADOR
}