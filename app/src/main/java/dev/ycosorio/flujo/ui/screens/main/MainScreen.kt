package dev.ycosorio.flujo.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.components.MainTopAppBar
import dev.ycosorio.flujo.ui.navigation.BottomNavItem
import dev.ycosorio.flujo.ui.navigation.Routes
import dev.ycosorio.flujo.ui.screens.admin.dashboard.AdminDashboard
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestScreen
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestViewModel
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardViewModel
import dev.ycosorio.flujo.ui.screens.documents.DocumentScreen
import dev.ycosorio.flujo.ui.screens.worker.dashboard.WorkerDashboard
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestViewModel
import dev.ycosorio.flujo.utils.Resource
import timber.log.Timber


@Composable
fun MainScreen(
    externalNavController: NavHostController,
    onNavigateToUserManagement: () -> Unit = {},
    onNavigateToAuth: () -> Unit // Para redirigir al Login si es expulsado
) {
    val context = LocalContext.current
    val internalNavController = rememberNavController()

    val navBackStackEntry by internalNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val dashboardViewModel: DashboardViewModel = hiltViewModel()

    val userState by dashboardViewModel.userState.collectAsState()

    // Cargar usuario actual
    LaunchedEffect(Unit) {
        Timber.d("🔄 Cargando usuario actual")
        dashboardViewModel.loadCurrentUser()
    }

    Scaffold(
        topBar = {
            val user = (userState as? Resource.Success)?.data
                MainTopAppBar(
                    user = user,
                    onProfileClicked = {
                        externalNavController.navigate(Routes.Settings.route)
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
        Box(
            modifier = Modifier.padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = userState) {
                is Resource.Loading, is Resource.Idle -> {
                    CircularProgressIndicator()
                }
                is Resource.Success -> {
                    state.data?.let { user ->
                        Timber.d("✅ Usuario cargado: ${user.name}")
                        NavHost(
                            internalNavController,
                            startDestination = BottomNavItem.Dashboard.route
                        ) {
                            composable(BottomNavItem.Dashboard.route) {
                                RealDashboardContent(
                                    user = user,
                                    navController = externalNavController,
                                    onNavigateToUserManagement = onNavigateToUserManagement
                                )
                            }

                            composable(BottomNavItem.Documents.route) {
                                DocumentScreen(
                                    onNavigateToSignature = { assignmentId ->
                                        externalNavController.navigate(Routes.Signature.createRoute(assignmentId))
                                    },
                                    navController = externalNavController
                                )
                            }

                            composable(BottomNavItem.Inventory.route) {
                                when (user.role) {
                                    Role.ADMINISTRADOR -> {
                                        val adminInventoryViewModel: MaterialRequestViewModel = hiltViewModel()
                                        MaterialRequestScreen(viewModel = adminInventoryViewModel,
                                            navController = externalNavController
                                        )
                                    }
                                    Role.TRABAJADOR -> {
                                        val workerInventoryViewModel: WorkerRequestViewModel = hiltViewModel()
                                        WorkerRequestScreen(
                                            viewModel = workerInventoryViewModel,
                                            navController = externalNavController,
                                            onAddRequestClicked = {
                                                externalNavController.navigate(Routes.CreateRequest.route)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            "Error al cargar usuario",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(state.message ?: "Error desconocido")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onNavigateToAuth) {
                            Text("Volver a Login")
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun RealDashboardContent(
    user: User,
    navController: NavHostController,
    onNavigateToUserManagement: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¡Hola, ${user.name}!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Bienvenido a Flujo",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // El contenido del dashboard cambia según el rol del usuario
        when (user.role) {
            Role.ADMINISTRADOR -> AdminDashboard(
                navController = navController,
                onNavigateToUserManagement = onNavigateToUserManagement,
                user = user
            )
            Role.TRABAJADOR -> WorkerDashboard(
                user = user,
                navController = navController
            )
        }
    }
}