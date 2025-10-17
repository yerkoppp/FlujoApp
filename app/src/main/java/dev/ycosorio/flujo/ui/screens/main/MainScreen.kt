package dev.ycosorio.flujo.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import dev.ycosorio.flujo.ui.components.MainTopAppBar
import dev.ycosorio.flujo.ui.navigation.BottomNavItem
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardScreen
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardViewModel
import dev.ycosorio.flujo.utils.Resource

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val dashboardViewModel: DashboardViewModel = viewModel()
    val userState by dashboardViewModel.userState.collectAsState()

    Scaffold(
        topBar = {
            MainTopAppBar(
                user = (userState as? Resource.Success)?.data,
                onProfileClicked = { /* navController.navigate("profile") */ },
                onSignOutClicked = { /* Lógica para cerrar sesión */ }
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
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(BottomNavItem.Documents.route) {
                // Aquí irá la pantalla de Documentos
                Text("Pantalla de Documentos")
            }
            composable(BottomNavItem.Inventory.route) {
                // Aquí decidiremos qué pantalla de inventario mostrar
                Text("Pantalla de Inventario")
            }
            // La pantalla de perfil ahora se navegaría por separado, no desde la BottomBar
            // composable("profile") { ProfileScreen() }
        }
    }
}