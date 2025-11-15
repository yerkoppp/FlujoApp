package dev.ycosorio.flujo.ui.screens.admin.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NoCrash
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.navigation.Routes
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardAction
import dev.ycosorio.flujo.ui.screens.dashboard.DashboardCard


@Composable
fun AdminDashboard(
    navController: NavHostController,
    onNavigateToUserManagement: () -> Unit,
    user: User
) {

    val dashboardActions = listOf(
        DashboardAction(
            title = "Trabajadores",
            icon = Icons.Default.PersonAddAlt1,
            onClick = onNavigateToUserManagement
        ),
        DashboardAction(
            title = "Vehículos",
            icon = Icons.Default.NoCrash,
            onClick = { navController.navigate(Routes.VehicleManagement.route) }
        ),
        DashboardAction(
            title = "Materiales",
            icon = Icons.Default.Inventory,
            onClick = { navController.navigate(Routes.MaterialManagement.route) }
        ),
        DashboardAction(
            title = "Bodegas",
            icon = Icons.Default.Warehouse,
            onClick = { navController.navigate(Routes.WarehouseManagement.route) }
        ),
        DashboardAction(
            title = "Rendiciones",
            icon = Icons.Default.MonetizationOn,
            onClick = { navController.navigate(Routes.ExpenseReportList.route) }
        ),
        DashboardAction(
            title = "Mensajes",
            icon = Icons.AutoMirrored.Filled.Message,
            onClick = {
                navController.navigate(Routes.Messages.createRoute(user.uid))

            }
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2 columnas
        verticalArrangement = Arrangement.spacedBy(15.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        items(dashboardActions) { action ->
            DashboardCard(action = action)
        }
    }
}

