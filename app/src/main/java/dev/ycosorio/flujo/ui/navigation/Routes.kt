package dev.ycosorio.flujo.ui.navigation

/**
 * Define las rutas de navegación de la aplicación de forma segura.
 */
sealed class Routes(val route: String) {
    object UserManagement : Routes("admin/users")
    object AddUser : Routes("admin/users/add")
    object UserDetail : Routes("admin/users/detail/{userId}") {
        fun createRoute(userId: String) = "admin/users/detail/$userId"
    }
    object EditUser : Routes("admin/users/edit/{userId}") {
        fun createRoute(userId: String) = "admin/users/edit/$userId"
    }
    object MaterialRequests : Routes("admin/inventory/requests")
    object WorkerRequests : Routes("worker/requests")
    object CreateRequest : Routes("worker/requests/new")
}