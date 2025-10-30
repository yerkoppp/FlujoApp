package dev.ycosorio.flujo.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestScreen
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestViewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.ycosorio.flujo.ui.screens.admin.users.EditUserScreen
import dev.ycosorio.flujo.ui.screens.admin.users.EditUserViewModel
import dev.ycosorio.flujo.ui.screens.admin.users.UserDetailScreen
import dev.ycosorio.flujo.ui.screens.main.MainScreen
import dev.ycosorio.flujo.ui.screens.admin.users.adduser.AddUserScreen
import dev.ycosorio.flujo.ui.screens.admin.users.usermanagament.UserManagementViewModel
import dev.ycosorio.flujo.ui.screens.admin.users.adduser.AddUserViewModel
import dev.ycosorio.flujo.ui.screens.admin.users.usermanagament.UserManagementScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.CreateRequestScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestViewModel
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.ui.screens.documents.SignatureScreen
import dev.ycosorio.flujo.ui.screens.documents.SignatureViewModel

@Composable
fun AppNavigation() {

    // Por ahora, vamos directamente a la pantalla principal.
    //MainScreen()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.WorkerRequests.route
    ) {
        // Pantalla principal con BottomNav
        composable("main") {
            MainScreen(
                navController = navController,
                onNavigateToUserManagement = {
                    navController.navigate(Routes.UserManagement.route)
                }
            )
        }

        // --- GRAFO DE NAVEGACIÓN DEL ADMINISTRADOR ---

        // Pantalla de Gestión de Usuarios
        composable(Routes.UserManagement.route) {
            val viewModel: UserManagementViewModel = hiltViewModel()
            UserManagementScreen(
                viewModel = viewModel,
                onAddUserClicked = {
                    navController.navigate(Routes.AddUser.route)
                },
                onBackPressed = {
                    navController.popBackStack()
                },
                onUserClicked = { user ->
                    navController.navigate(Routes.UserDetail.createRoute(user.uid))
                }
            )
        }

        // Pantalla para Añadir un Usuario
        composable(Routes.AddUser.route) {
            val viewModel: AddUserViewModel = hiltViewModel()
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

        // Pantalla de Detalle de Usuario
        composable(
            route = "admin/users/detail/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable

            // Cargar el usuario desde Firebase
            val viewModel: UserManagementViewModel = hiltViewModel()
            val editViewModel: EditUserViewModel = hiltViewModel()
            val usersState by viewModel.usersState.collectAsState()
            val deleteState by editViewModel.deleteUserState.collectAsState()

            LaunchedEffect(deleteState) {
                if (deleteState is Resource.Success) {
                    navController.popBackStack(Routes.UserManagement.route, false)
                }
            }

            when (val state = usersState) {
                is Resource.Idle, is Resource.Loading -> CircularProgressIndicator()
                is Resource.Success -> {
                    val user = state.data?.find { it.uid == userId }
                    if (user != null) {
                        UserDetailScreen(
                            user = user,
                            onBackPressed = { navController.popBackStack() },
                            onEditClicked = {
                                navController.navigate(Routes.EditUser.createRoute(user.uid))
                            },
                            onDeleteClicked = { userToDelete ->
                                editViewModel.deleteUser(userToDelete.uid)
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ){
                        Text("Usuario no encontrado")
                        }
                    }
                }
                else -> CircularProgressIndicator()
            }
        }

// Pantalla de Editar Usuario
        composable(
            route = "admin/users/edit/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable

            val userViewModel: UserManagementViewModel = hiltViewModel()
            val editViewModel: EditUserViewModel = hiltViewModel()
            val usersState by userViewModel.usersState.collectAsState()

            when (val state = usersState) {
                is Resource.Success -> {
                    val user = state.data?.find { it.uid == userId }
                    if (user != null) {
                        EditUserScreen(
                            user = user,
                            viewModel = editViewModel,
                            onUserUpdatedSuccessfully = {
                                navController.popBackStack()
                            },
                            onBackPressed = {
                                navController.popBackStack()
                            }
                        )
                    } else {
                        Text("Usuario no encontrado")
                    }
                }
                else -> CircularProgressIndicator()
            }
        }

        // Pantalla para Solicitudes de Materiales
        composable(Routes.MaterialRequests.route) {
            val viewModel: MaterialRequestViewModel = hiltViewModel()
            MaterialRequestScreen(viewModel = viewModel)
        }

        // --- GRAFO DE NAVEGACIÓN DEL TRABAJADOR ---

        // Pantalla de la lista de solicitudes del trabajador
        composable(Routes.WorkerRequests.route) {
            val viewModel: WorkerRequestViewModel = hiltViewModel()
            WorkerRequestScreen(
                viewModel = viewModel,
                onAddRequestClicked = {
                    navController.navigate(Routes.CreateRequest.route)
                }
            )
        }

        // Pantalla para crear una nueva solicitud
        composable(Routes.CreateRequest.route) {
            val viewModel: WorkerRequestViewModel = hiltViewModel()
            CreateRequestScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.popBackStack() // Volver a la lista
                }
            )
        }

        composable(
            route = Routes.Signature.route,
            arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
        ) {
            // Hilt proveerá el ViewModel y el assignmentId
            SignatureScreen(
                onSignatureSaved = { navController.popBackStack() },
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}