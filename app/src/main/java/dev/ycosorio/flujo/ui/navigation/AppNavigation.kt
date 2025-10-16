package dev.ycosorio.flujo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ycosorio.flujo.ui.screens.admin.users.AddUserScreen
import dev.ycosorio.flujo.ui.screens.admin.users.UserManagementViewModel
import dev.ycosorio.flujo.ui.screens.admin.users.AddUserViewModel
import dev.ycosorio.flujo.ui.screens.admin.users.UserManagementScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.UserManagement.route
    ) {
        // Pantalla de Gestión de Usuarios
        composable(Routes.UserManagement.route) {
            val viewModel: UserManagementViewModel = viewModel()
            UserManagementScreen(
                viewModel = viewModel,
                onAddUserClicked = {
                    navController.navigate(Routes.AddUser.route)
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
                }
            )
        }
    }
}