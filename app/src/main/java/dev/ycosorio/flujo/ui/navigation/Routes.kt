package dev.ycosorio.flujo.ui.navigation

/**
 * Define las rutas de navegación de la aplicación de forma segura.
 */
sealed class Routes(val route: String) {
    object UserManagement : Routes("admin/users")
    object AddUser : Routes("admin/users/add")
    object MaterialRequests : Routes("admin/inventory/requests")
}