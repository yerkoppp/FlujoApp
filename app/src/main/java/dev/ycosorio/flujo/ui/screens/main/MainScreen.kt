package dev.ycosorio.flujo.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.domain.model.Role
import java.util.Date
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import dev.ycosorio.flujo.ui.components.MainTopAppBar
import dev.ycosorio.flujo.ui.navigation.BottomNavItem
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardScreen
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardViewModel
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.ui.navigation.Routes
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.ui.screens.documents.DocumentScreen

@Composable
fun MainScreen(
    navController: NavHostController,
    onNavigateToUserManagement: () -> Unit = {}
) {
    //val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    //val dashboardViewModel: DashboardViewModel = viewModel()

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

    //val userState by dashboardViewModel.userState.collectAsState()

    Scaffold(
        topBar = {
            MainTopAppBar(
                user = (userState as? Resource.Success)?.data,
                onProfileClicked = { /* navController.navigate("profile") */ },
                onSignOutClicked = { /* Lógica para cerrar sesión */ },
                onUserManagementClicked = onNavigateToUserManagement

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
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            navController,
            startDestination = BottomNavItem.Dashboard.route,
            Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) {
                //DashboardScreen(viewModel = dashboardViewModel)
                Text("Dashboard - Por implementar")
            }

            composable(BottomNavItem.Documents.route) {
                DocumentScreen(onNavigateToSignature = { assignmentId ->
                    navController.navigate(Routes.Signature.createRoute(assignmentId))
                }) // Hilt se encargará de proveer el ViewModel
            }
            composable(BottomNavItem.Inventory.route) {
                // Aquí decidiremos qué pantalla de inventario mostrarval currentUser = (userState as? Resource.Success)?.data
                val currentUser = (userState as? Resource.Success)?.data
                when (currentUser?.role) {
                    Role.ADMINISTRADOR -> {
                        Text("Pantalla Admin Inventario - Por implementar")
                        // MaterialRequestScreen(viewModel)
                    }
                    Role.TRABAJADOR -> {
                        Text("Pantalla Worker Inventario - Por implementar")
                        // WorkerRequestScreen(viewModel)
                    }
                    else -> {
                        Text("Cargando...")
                    }
                }
            }
            // La pantalla de perfil ahora se navegaría por separado, no desde la BottomBar
            // composable("profile") { ProfileScreen() }
        }
    }
}