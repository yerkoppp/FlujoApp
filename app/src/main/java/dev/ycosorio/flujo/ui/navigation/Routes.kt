package dev.ycosorio.flujo.ui.navigation

/**
 * Define las rutas de navegación de la aplicación de forma segura.
 */
sealed class Routes(val route: String) {

    object Login : Routes("login")
    object Main : Routes("main")
    object AccessVerification : Routes("verify_access")

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
    object Signature : Routes("documents/{assignmentId}/sign") {
        fun createRoute(assignmentId: String) = "documents/$assignmentId/sign"
    }
    object Settings : Routes("settings")
    object AssignDocument : Routes("admin/documents/assign/{templateId}") {
        fun createRoute(templateId: String) = "admin/documents/assign/$templateId"
    }
    object UploadTemplate : Routes("admin/documents/upload")
    object Profile : Routes("profile")
    object EditProfile : Routes("profile/edit")
    object Appearance : Routes("settings/appearance")
    object VehicleManagement: Routes ("vehicle_management" )
    object MaterialManagement: Routes ( "material_management" )
}