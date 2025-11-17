package dev.ycosorio.flujo.ui.screens.admin.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NoCrash
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.ycosorio.flujo.domain.model.User
import dev.ycosorio.flujo.ui.navigation.Routes


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    navController: NavHostController,
    onNavigateToUserManagement: () -> Unit,
    user: User,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val dashboardActions = listOf(
        DashboardAction(
            title = "Trabajadores",
            icon = Icons.Default.PersonAddAlt1,
            onClick = onNavigateToUserManagement,
            insight = "15 Activos"
        ),
        DashboardAction(
            title = "Vehículos",
            icon = Icons.Default.NoCrash,
            onClick = { navController.navigate(Routes.VehicleManagement.route) },
            insight = "4 en Taller"
        ),
        DashboardAction(
            title = "Materiales",
            icon = Icons.Default.Inventory,
            onClick = { navController.navigate(Routes.MaterialManagement.route) },
            insight = "12 Solicitudes"
        ),
        DashboardAction(
            title = "Bodegas",
            icon = Icons.Default.Warehouse,
            onClick = { navController.navigate(Routes.WarehouseManagement.route) },
            insight = "3 Stock Bajo"
        ),
        DashboardAction(
            title = "Rendiciones",
            icon = Icons.Default.MonetizationOn,
            onClick = { navController.navigate(Routes.AdminExpenseReports.route) },
            insight = "8 Pendientes"
        ),
        DashboardAction(
            title = "Mensajes",
            icon = Icons.AutoMirrored.Filled.Message,
            onClick = { navController.navigate(Routes.Messages.createRoute(user.uid))},
            insight = "2 Sin Leer"
        ),
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).fillMaxWidth(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bienvenido a Flujo",
                        fontWeight = FontWeight.Bold // Un toque más de énfasis
                    )
                },
                actions = {
                    NotificationBadge(
                        count = uiState.pendingDocuments.size,
                        tint = MaterialTheme.colorScheme.primary
                    ) {
                        navController.navigate(Routes.Notifications.createRoute(user.uid))
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Error")
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .padding(paddingValues),
                columns = GridCells.Fixed(2), // 2 columnas
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(20.dp),
            ) {
                items(
                    items = dashboardActions,
                    key = { it.title }
                ) { action ->
                    DashboardCard(action = action)
                }
            }
        }
    }
}
@Composable
private fun NotificationBadge(
    count: Int,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.clickable { onClick() }.padding(end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Notificaciones",
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

