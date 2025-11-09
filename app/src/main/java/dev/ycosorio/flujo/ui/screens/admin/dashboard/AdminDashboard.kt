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
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.ui.navigation.Routes


@Composable
fun AdminDashboard(
    navController: NavHostController,
    onNavigateToUserManagement: () -> Unit
) {
    val adminActions = listOf(
        AdminAction(
            title = "Gestionar de Usuarios",
            icon = Icons.Default.Person,
            onClick = onNavigateToUserManagement
        ),
        AdminAction(
            title = "Gestionar Vehículos",
            icon = Icons.Default.NoCrash,
            onClick = { navController.navigate(Routes.VehicleManagement.route) }
        ),
        AdminAction(
            title = "Gestionar Materiales",
            icon = Icons.Default.Inventory,
            onClick = { navController.navigate(Routes.MaterialManagement.route) }
        ),
        AdminAction(
            title = "Gestionar Bodegas",
            icon = Icons.Default.Warehouse,
            onClick = { navController.navigate(Routes.WarehouseManagement.route) }
        ),
        AdminAction(
            title = "Notificaciones",
            icon = Icons.Default.NotificationsActive,
            onClick = { /* TODO: Navegar a pantalla de notificaciones */ }
        ),
        AdminAction(
            title = "Enviar Mensaje",
            icon = Icons.AutoMirrored.Filled.Message,
            onClick = { /* TODO: Navegar a pantalla de enviar mensaje */ }
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2 columnas
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(adminActions) { action ->
            AdminDashboardCard(action = action)
        }
    }
}

