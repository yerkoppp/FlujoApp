package dev.ycosorio.flujo.ui.navigation

import dev.ycosorio.flujo.ui.screens.worker.inventory.CreateRequestViewModel
import android.util.Log
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
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.ycosorio.flujo.ui.AppViewModel
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialManagementScreen
import dev.ycosorio.flujo.ui.screens.admin.users.EditUserScreen
import dev.ycosorio.flujo.ui.screens.admin.users.EditUserViewModel
import dev.ycosorio.flujo.ui.screens.admin.users.UserDetailScreen
import dev.ycosorio.flujo.ui.screens.admin.users.adduser.AddUserScreen
import dev.ycosorio.flujo.ui.screens.admin.users.adduser.AddUserViewModel
import dev.ycosorio.flujo.ui.screens.admin.users.usermanagament.UserManagementScreen
import dev.ycosorio.flujo.ui.screens.admin.users.usermanagament.UserManagementViewModel
import dev.ycosorio.flujo.ui.screens.admin.vehicles.VehicleManagementScreen
import dev.ycosorio.flujo.ui.screens.admin.warehouse.WarehouseManagementScreen
import dev.ycosorio.flujo.ui.screens.auth.AccessVerificationScreen
import dev.ycosorio.flujo.ui.screens.auth.LoginScreen
import dev.ycosorio.flujo.ui.screens.documents.AssignDocumentScreen
import dev.ycosorio.flujo.ui.screens.documents.DocumentDetailScreen
import dev.ycosorio.flujo.ui.screens.documents.SignatureScreen
import dev.ycosorio.flujo.ui.screens.documents.UploadTemplateScreen
import dev.ycosorio.flujo.ui.screens.main.MainScreen
import dev.ycosorio.flujo.ui.screens.profile.EditProfileScreen
import dev.ycosorio.flujo.ui.screens.profile.ProfileScreen
import dev.ycosorio.flujo.ui.screens.settings.AppearanceScreen
import dev.ycosorio.flujo.ui.screens.settings.SettingsScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.CreateRequestScreen
import dev.ycosorio.flujo.utils.Resource

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val currentAuthUser by appViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.AccessVerification.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.AccessVerification.route) {
            AccessVerificationScreen(
                authUser = currentAuthUser,
                onAccessGranted = {
                    Log.d("AppNavigation", "✅ Navegando a Main")
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.AccessVerification.route) { inclusive = true }
                    }
                },
                onAccessDenied = {
                    Log.d("AppNavigation", "❌ Navegando a Login")
                    // Limpiar el estado del usuario
                    appViewModel.clearUserProfile()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.AccessVerification.route) { inclusive = true }
                    }
                }
            )
        }
        // Pantalla principal con BottomNav
        composable(Routes.Main.route) {
            MainScreen(
                externalNavController = navController,
                onNavigateToUserManagement = {
                    navController.navigate(Routes.UserManagement.route)
                },
                onNavigateToAuth = {
                    // Limpiar el estado antes de navegar a auth
                    appViewModel.clearUserProfile()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Main.route) { inclusive = true }
                    }
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
        composable(Routes.VehicleManagement.route) {
            VehicleManagementScreen(navController = navController)
        }
        composable(Routes.MaterialManagement.route) {
            MaterialManagementScreen(navController = navController)
        }
        composable(Routes.WarehouseManagement.route) {
            WarehouseManagementScreen(navController = navController)
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
        // --- GRAFO DE NAVEGACIÓN DEL TRABAJADOR ---
        // Pantalla para crear una nueva solicitud
        composable(Routes.CreateRequest.route) {
            val viewModel: CreateRequestViewModel = hiltViewModel()
            CreateRequestScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.popBackStack() // Volver a la lista
                }
            )
        }
        // Pantalla de Firma
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

        // Documento asignado
        composable(
            route = Routes.AssignDocument.route,
            arguments = listOf(navArgument("templateId") { type = NavType.StringType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: return@composable

            AssignDocumentScreen(
                templateId = templateId,
                onBackPressed = { navController.popBackStack() },
                onAssignSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DocumentDetail.route,
            arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
        ) {
            val assignmentId = it.arguments?.getString("assignmentId") ?: return@composable
            DocumentDetailScreen(
                assignmentId = assignmentId,
                onBackPressed = { navController.navigateUp() },
                onNavigateToSignature = {
                    navController.navigate(Routes.Signature.createRoute(assignmentId))
                }
            )
        }

        composable(Routes.UploadTemplate.route) {
            UploadTemplateScreen(
                onBackPressed = { navController.popBackStack() },
                onUploadSuccess = { navController.popBackStack() }
            )
        }

        // Pantalla de Configuración
        composable(Routes.Settings.route) {
            SettingsScreen(
                onBackPressed = { navController.popBackStack() },
                onNavigateToAuth = {
                    // Limpiar estado antes de navegar
                    appViewModel.clearUserProfile()
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.Profile.route)
                },
                onNavigateToAppearance = {
                    navController.navigate(Routes.Appearance.route)
                }
            )
        }
        composable(Routes.Appearance.route) {
            AppearanceScreen(
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable(Routes.Profile.route) {
            ProfileScreen(
                onBackPressed = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Routes.EditProfile.route) }
            )
        }

        composable(Routes.EditProfile.route) {
            EditProfileScreen(
                onBackPressed = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }
    }
}