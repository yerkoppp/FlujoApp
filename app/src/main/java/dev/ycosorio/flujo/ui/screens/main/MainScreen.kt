package dev.ycosorio.flujo.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
//import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.ui.components.MainTopAppBar
import dev.ycosorio.flujo.ui.navigation.BottomNavItem
import dev.ycosorio.flujo.ui.navigation.Routes
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestScreen
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestViewModel
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardScreen
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardViewModel
import dev.ycosorio.flujo.ui.screens.documents.DocumentScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestViewModel
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.utils.SimulationAuth

@Composable
fun MainScreen(
    externalNavController: NavHostController,
    onNavigateToUserManagement: () -> Unit = {}
) {
    val internalNavController = rememberNavController()

    val navBackStackEntry by internalNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
/*
    val mockUser = remember {
        User(
            uid = "admin_001",
            name = "Admin Prueba",
            email = "admin@flujo.com",
            phoneNumber = "+56912345678",
            photoUrl = null,
            role = Role.ADMINISTRADOR,
            position = "Gerente General",
            area = "Administración",
            contractStartDate = Date(),
            contractEndDate = null
        )
    }

    val userState = Resource.Success(mockUser)
*/

    val userState by dashboardViewModel.userState.collectAsState()

    Scaffold(
        topBar = {
            MainTopAppBar(
                user = (userState as? Resource.Success)?.data,
                onProfileClicked = { /* navController.navigate("profile") */ },
                onSignOutClicked = { /* Lógica para cerrar sesión */ },
                onUserManagementClicked = onNavigateToUserManagement,
                onToggleUser = {
                    SimulationAuth.toggleUser()
                }

            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    BottomNavItem.Dashboard,
                    BottomNavItem.Documents,
                    BottomNavItem.Inventory
                )
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            internalNavController.navigate(item.route) {
                                popUpTo(internalNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            internalNavController,
            startDestination = BottomNavItem.Dashboard.route,
            Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen(viewModel = dashboardViewModel)
            }

            composable(BottomNavItem.Documents.route) {
                DocumentScreen(onNavigateToSignature = { assignmentId ->
                    externalNavController.navigate(Routes.Signature.createRoute(assignmentId))
                }) // Hilt se encargará de proveer el ViewModel
            }
            composable(BottomNavItem.Inventory.route) {
                // Aquí decidiremos qué pantalla de inventario mostrar
                val currentUser = (userState as? Resource.Success)?.data
                when (currentUser?.role) {
                    Role.ADMINISTRADOR -> {
                        // Carga la pantalla del Admin
                        val adminInventoryViewModel: MaterialRequestViewModel = hiltViewModel()
                        MaterialRequestScreen(viewModel = adminInventoryViewModel)
                    }
                    Role.TRABAJADOR -> {
                        // Carga la pantalla del Trabajador
                        val workerInventoryViewModel: WorkerRequestViewModel = hiltViewModel()
                        WorkerRequestScreen(
                            viewModel = workerInventoryViewModel,
                            onAddRequestClicked = {
                                // Navega a la pantalla de crear solicitud
                                externalNavController.navigate(Routes.CreateRequest.route)
                            }
                        )
                    }
                    else -> {
                        Text("Cargando...")
                    }
                }
            }
        }
    }
}