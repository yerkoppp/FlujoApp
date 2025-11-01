package dev.ycosorio.flujo.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.firebase.ui.auth.AuthUI
import dev.ycosorio.flujo.domain.model.Role
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.components.MainTopAppBar
import dev.ycosorio.flujo.ui.navigation.BottomNavItem
import dev.ycosorio.flujo.ui.navigation.Routes
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestScreen
import dev.ycosorio.flujo.ui.screens.admin.inventory.MaterialRequestViewModel
import dev.ycosorio.flujo.ui.screens.auth.LoginViewModel
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardScreen
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardViewModel
import dev.ycosorio.flujo.ui.screens.documents.DocumentScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestScreen
import dev.ycosorio.flujo.ui.screens.worker.inventory.WorkerRequestViewModel
import dev.ycosorio.flujo.utils.Resource
import dev.ycosorio.flujo.utils.SimulationAuth
import java.util.Date

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
    val loginViewModel: LoginViewModel = hiltViewModel()

    var isAuthorized by remember { mutableStateOf(false) }
    var authorizedUser by remember { mutableStateOf<User?>(null) }
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
            if (isAuthorized) {
                MainTopAppBar(
                    user = authorizedUser,
                    onProfileClicked = { /* navController.navigate("profile") */ },
                    onSignOutClicked = {
                        loginViewModel.signOut()
                        AuthUI.getInstance()
                            .signOut(context)
                            .addOnCompleteListener {
                                onNavigateToAuth()
                            }
                        /*externalNavController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }*/
                    },
                    onUserManagementClicked = onNavigateToUserManagement
                    /*onToggleUser = {
                        SimulationAuth.toggleUser()
                    }*/
                )
            }
        },
        bottomBar = {
            // Solo muestra la BottomBar si el usuario está autorizado
            if (isAuthorized) {
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
        }
    ) { innerPadding ->
        if (!isAuthorized) {
            // 1. AÚN NO AUTORIZADO: Muestra la pantalla de "Control de Acceso"
            DashboardScreen(
                viewModel = dashboardViewModel,
                onUserAuthorized = { user ->
                    // ¡Autorizado! Actualizamos el estado
                    authorizedUser = user
                    isAuthorized = true
                },
                onUserUnauthorized = {
                    // ¡Expulsado! Notificamos a AppNavigation para que vaya al Login
                    // FirebaseAuth.getInstance().signOut() // Asegúrate de cerrar sesión
                    loginViewModel.signOut()
                    onNavigateToAuth()
                }
            )
        } else {
            // 2. ¡AUTORIZADO! Muestra el contenido normal de la app
            NavHost(
                internalNavController,
                startDestination = BottomNavItem.Dashboard.route,
                Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Dashboard.route) {
                    //DashboardScreen(viewModel = dashboardViewModel)
                    RealDashboardContent(user = authorizedUser!!)
                }

                composable(BottomNavItem.Documents.route) {
                    DocumentScreen(onNavigateToSignature = { assignmentId ->
                        externalNavController.navigate(Routes.Signature.createRoute(assignmentId))
                    }) // Hilt se encargará de proveer el ViewModel
                }
                composable(BottomNavItem.Inventory.route) {
                    // Aquí decidiremos qué pantalla de inventario mostrar
                    val currentUser = authorizedUser
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
}


@Composable
private fun RealDashboardContent(user: User) {
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
            Role.ADMINISTRADOR -> AdminDashboard()
            Role.TRABAJADOR -> WorkerDashboard()
        }
    }
}


@Composable
fun AdminDashboard() {
    // Aquí irán los componentes específicos para el administrador
    // Por ejemplo, un resumen de solicitudes pendientes, usuarios activos, etc.
    Text("Vista del Administrador", style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun WorkerDashboard() {
    // Aquí irán los componentes específicos para el trabajador
    // Por ejemplo, documentos pendientes de firma, estado de su última solicitud, etc.
    Text("Vista del Trabajador", style = MaterialTheme.typography.bodyLarge)
}

