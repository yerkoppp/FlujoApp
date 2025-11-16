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
    object AdminExpenseReports : Routes("admin/expenses")
    object UserDetail : Routes("admin/users/detail/{userId}") {
        fun createRoute(userId: String) = "admin/users/detail/$userId"
    }
    object EditUser : Routes("admin/users/edit/{userId}") {
        fun createRoute(userId: String) = "admin/users/edit/$userId"
    }
    object MaterialRequests : Routes("admin/inventory/requests")
    object WorkerRequests : Routes("worker/requests")
    object CreateRequest : Routes("worker/requests/new")
    object DocumentDetail : Routes("documents/detail/{assignmentId}") {
        fun createRoute(assignmentId: String) = "documents/detail/$assignmentId"
    }

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
    object WarehouseManagement: Routes("warehouse_management")
    object Messages : Routes("messages/{userId}") {
        fun createRoute(userId: String) = "messages/$userId"
    }
    object ComposeMessage : Routes("compose_message/{userId}/{userName}/{userRole}") {
        fun createRoute(userId: String, userName: String, userRole: String) =
            "compose_message/$userId/$userName/$userRole"
    }
    object ExpenseReportList : Routes("worker/expenses")
    object CreateExpenseReport : Routes("worker/expenses/new")
    object EditExpenseReport : Routes("worker/expenses/edit/{reportId}") {
        fun createRoute(reportId: String) = "worker/expenses/edit/$reportId"
    }
    object Notifications : Routes("notifications/{userId}") {
        fun createRoute(userId: String) = "notifications/$userId"
    }
}

