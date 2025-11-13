package dev.ycosorio.flujo.ui.screens.admin.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NoCrash
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.AppViewModel
import dev.ycosorio.flujo.ui.navigation.Routes


@Composable
fun AdminDashboard(
    navController: NavHostController,
    onNavigateToUserManagement: () -> Unit,
    viewModel: AppViewModel = hiltViewModel()
) {

    val currentUser by viewModel.currentUserProfile.collectAsState()

    val adminActions = listOf(
        AdminAction(
            title = "Usuarios",
            icon = Icons.Default.PersonAddAlt1,
            onClick = onNavigateToUserManagement
        ),
        AdminAction(
            title = "Vehículos",
            icon = Icons.Default.NoCrash,
            onClick = { navController.navigate(Routes.VehicleManagement.route) }
        ),
        AdminAction(
            title = "Materiales",
            icon = Icons.Default.Inventory,
            onClick = { navController.navigate(Routes.MaterialManagement.route) }
        ),
        AdminAction(
            title = "Bodegas",
            icon = Icons.Default.Warehouse,
            onClick = { navController.navigate(Routes.WarehouseManagement.route) }
        ),
        AdminAction(
            title = "Notificaciones",
            icon = Icons.Default.NotificationsActive,
            onClick = { /* TODO: Navegar a pantalla de notificaciones */ }
        ),
        AdminAction(
            title = "Mensajes",
            icon = Icons.AutoMirrored.Filled.Message,
            onClick = {
                currentUser?.let { user ->
                    navController.navigate(Routes.Messages.createRoute(user.uid))
                }
            }
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2 columnas
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(adminActions) { action ->
            AdminDashboardCard(action = action)
        }
    }
}

