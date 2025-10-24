package dev.ycosorio.flujo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestScreen
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ycosorio.flujo.ui.screens.main.MainScreen
import dev.ycosorio.flujo.ui.screens.admin.users.AddUserScreen
import dev.ycosorio.flujo.ui.screens.admin.users.UserManagementViewModel
import dev.ycosorio.flujo.ui.screens.admin.users.AddUserViewModel
import dev.ycosorio.flujo.ui.screens.admin.users.UserManagementScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.CreateRequestScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestViewModel

@Composable
fun AppNavigation() {

    // Por ahora, vamos directamente a la pantalla principal.
    //MainScreen()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // Pantalla principal con BottomNav
        composable("main") {
            MainScreen(
                onNavigateToUserManagement = {
                    navController.navigate(Routes.UserManagement.route)
                }
            )
        }

        // --- GRAFO DE NAVEGACIÓN DEL ADMINISTRADOR ---

        // Pantalla de Gestión de Usuarios
        composable(Routes.UserManagement.route) {
            val viewModel: UserManagementViewModel = viewModel()
            UserManagementScreen(
                viewModel = viewModel,
                onAddUserClicked = {
                    navController.navigate(Routes.AddUser.route)
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla para Añadir un Usuario
        composable(Routes.AddUser.route) {
            val viewModel: AddUserViewModel = viewModel()
            AddUserScreen(
                viewModel = viewModel,
                onUserAddedSuccessfully = {
                    navController.popBackStack() // Regresa a la pantalla anterior
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla para Solicitudes de Materiales
        composable(Routes.MaterialRequests.route) {
            val viewModel: MaterialRequestViewModel = viewModel()
            MaterialRequestScreen(viewModel = viewModel)
        }

        // --- GRAFO DE NAVEGACIÓN DEL TRABAJADOR ---

        // Pantalla de la lista de solicitudes del trabajador
        composable(Routes.WorkerRequests.route) {
            val viewModel: WorkerRequestViewModel = viewModel()
            WorkerRequestScreen(
                viewModel = viewModel,
                onAddRequestClicked = {
                    navController.navigate(Routes.CreateRequest.route)
                }
            )
        }

        // Pantalla para crear una nueva solicitud
        composable(Routes.CreateRequest.route) {
            val viewModel: WorkerRequestViewModel = viewModel()
            CreateRequestScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.popBackStack() // Volver a la lista
                }
            )
        }
    }
}